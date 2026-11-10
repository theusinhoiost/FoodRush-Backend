package com.foodrush.api.service;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.request.RestaurantPatchRequestDTO;
import com.foodrush.api.dto.request.RestaurantRequestDTO;
import com.foodrush.api.dto.response.RestaurantResponseDTO;
import com.foodrush.api.model.Address;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private RestaurantService restaurantService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve cadastrar um restaurante com sucesso resolvendo endereço e coordenadas pelo CEP")
    void shouldCreateRestaurantSuccessfully() {
        // Arrange (Cenário)
        UUID ownerId = UUID.randomUUID();
        AddressRequestDTO addressDTO = new AddressRequestDTO(
                "01001-000",
                "100",
                "Loja 1"
        );
        RestaurantRequestDTO requestDTO = new RestaurantRequestDTO(
                ownerId,
                "Burger King - Centro",
                "48555407000133",
                "11999998888",
                new BigDecimal("5.00"),
                addressDTO
        );

        User ownerMock = new User();
        ownerMock.setId(ownerId);

        Address resolvedAddress = new Address();
        resolvedAddress.setId(UUID.randomUUID());
        resolvedAddress.setCep("01001000");
        resolvedAddress.setStreet("Praça da Sé");
        resolvedAddress.setNumber("100");
        resolvedAddress.setNeighborhood("Sé");
        resolvedAddress.setCity("São Paulo");
        resolvedAddress.setState("SP");
        resolvedAddress.setLatitude(new BigDecimal("-23.550520"));
        resolvedAddress.setLongitude(new BigDecimal("-46.633309"));

        Restaurant restaurantMock = new Restaurant();
        restaurantMock.setId(UUID.randomUUID());
        restaurantMock.setTradeName(requestDTO.tradeName());
        restaurantMock.setIsOpen(false);
        restaurantMock.setOwner(ownerMock);
        restaurantMock.setLatitude(resolvedAddress.getLatitude());
        restaurantMock.setLongitude(resolvedAddress.getLongitude());
        restaurantMock.setAddress(resolvedAddress);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(ownerMock));
        when(geocodingService.resolveAddress(addressDTO)).thenReturn(resolvedAddress);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurantMock);

        // Act (Ação)
        RestaurantResponseDTO response = restaurantService.create(requestDTO);

        // Assert (Validações)
        assertThat(response).isNotNull();
        assertThat(response.tradeName()).isEqualTo("Burger King - Centro");
        assertThat(response.address()).isNotNull();
        assertThat(response.address().street()).isEqualTo("Praça da Sé");
        assertThat(response.latitude()).isEqualByComparingTo("-23.550520");

        verify(userRepository, times(1)).findById(ownerId);
        verify(geocodingService, times(1)).resolveAddress(addressDTO);
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve permitir alternar status aberto/fechado se o usuário autenticado for o dono")
    void shouldToggleOpenStatusWhenOwner() {
        UUID restaurantId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("dono@restaurante.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);
        restaurant.setIsOpen(false);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantResponseDTO result = restaurantService.toggleOpenStatus(restaurantId, "dono@restaurante.com");

        assertThat(result.isOpen()).isTrue();
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao tentar alternar status de restaurante de outro dono")
    void shouldThrowWhenToggleOpenStatusNotOwner() {
        UUID restaurantId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("dono@restaurante.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);
        restaurant.setIsOpen(false);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.toggleOpenStatus(restaurantId, "invasor@restaurante.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não possui permissão");

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir alternar status se o usuário for ADMIN mesmo não sendo o dono")
    void shouldToggleOpenStatusWhenAdmin() {
        UUID restaurantId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("dono@restaurante.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);
        restaurant.setIsOpen(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@foodrush.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantResponseDTO result = restaurantService.toggleOpenStatus(restaurantId, "admin@foodrush.com");

        assertThat(result.isOpen()).isTrue();
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao tentar cadastrar restaurante para outro usuário sem ser ADMIN")
    void shouldThrowWhenCreatingRestaurantForAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        User ownerMock = new User();
        ownerMock.setId(ownerId);
        ownerMock.setEmail("outro@foodrush.com");

        AddressRequestDTO addressDTO = new AddressRequestDTO("01001-000", "100", "Loja 1");
        RestaurantRequestDTO requestDTO = new RestaurantRequestDTO(
                ownerId, "Pizzaria", "11222333000181", "11988887777", new BigDecimal("7.00"), addressDTO
        );

        when(restaurantRepository.existsByCnpj(any())).thenReturn(false);
        when(restaurantRepository.existsByPhone(any())).thenReturn(false);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(ownerMock));

        assertThatThrownBy(() -> restaurantService.create(requestDTO, "autenticado@foodrush.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Você só pode cadastrar um restaurante para o seu próprio usuário");
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao tentar transferir a propriedade do restaurante sem ser ADMIN")
    void shouldThrowWhenNonAdminTriesToTransferOwnershipInUpdate() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID newOwnerId = UUID.randomUUID();

        User owner = new User();
        owner.setId(ownerId);
        owner.setEmail("dono@restaurante.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);

        AddressRequestDTO addressDTO = new AddressRequestDTO("01001-000", "100", "Loja 1");
        RestaurantRequestDTO updateDTO = new RestaurantRequestDTO(
                newOwnerId, "Novo Nome", "48555407000133", "11999998888", new BigDecimal("5.00"), addressDTO
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.update(restaurantId, updateDTO, "dono@restaurante.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Apenas administradores podem transferir a propriedade");
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao tentar transferir a propriedade do restaurante via PATCH sem ser ADMIN")
    void shouldThrowWhenNonAdminTriesToTransferOwnershipInPatch() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID newOwnerId = UUID.randomUUID();

        User owner = new User();
        owner.setId(ownerId);
        owner.setEmail("dono@restaurante.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);

        RestaurantPatchRequestDTO patchDTO = new RestaurantPatchRequestDTO(
                newOwnerId, null, null, null, null, null, null
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.patch(restaurantId, patchDTO, "dono@restaurante.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Apenas administradores podem transferir a propriedade");
    }
}