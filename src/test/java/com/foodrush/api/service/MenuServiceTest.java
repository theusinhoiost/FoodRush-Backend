package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.ComplementGroupRequestDTO;
import com.foodrush.api.dto.request.ComplementItemRequestDTO;
import com.foodrush.api.dto.request.MenuItemRequestDTO;
import com.foodrush.api.dto.request.MenuItemSizeRequestDTO;
import com.foodrush.api.dto.response.MenuItemResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.MenuItem;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.MenuItemRepository;
import com.foodrush.api.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

  @Mock
  private MenuItemRepository menuItemRepository;

  @Mock
  private RestaurantRepository restaurantRepository;

  @InjectMocks
  private MenuService menuService;

  private UUID restaurantId;
  private Restaurant restaurant;

  @BeforeEach
  void setUp() {
    restaurantId = UUID.randomUUID();
    User owner = new User();
    owner.setId(UUID.randomUUID());
    owner.setEmail("dono@foodrush.com");

    restaurant = new Restaurant();
    restaurant.setId(restaurantId);
    restaurant.setTradeName("Burger Rush");
    restaurant.setOwner(owner);
  }

  @Test
  @DisplayName("Deve buscar cardápio completo de um restaurante")
  void shouldFindMenuByRestaurant() {
    MenuItem item = MenuItem.builder()
        .id(UUID.randomUUID())
        .restaurant(restaurant)
        .name("Cheeseburger")
        .basePrice(new BigDecimal("25.00"))
        .available(true)
        .sizes(new ArrayList<>())
        .complementGroups(new ArrayList<>())
        .build();

    when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
    when(menuItemRepository.findAllByRestaurantIdAndAvailableTrue(restaurantId)).thenReturn(List.of(item));
    when(menuItemRepository.findByIdWithSizes(item.getId())).thenReturn(Optional.of(item));
    when(menuItemRepository.findByIdWithComplements(item.getId())).thenReturn(Optional.of(item));

    List<MenuItemResponseDTO> menu = menuService.findMenuByRestaurant(restaurantId);

    assertThat(menu).hasSize(1);
    assertThat(menu.get(0).name()).isEqualTo("Cheeseburger");
    verify(menuItemRepository).findAllByRestaurantIdAndAvailableTrue(restaurantId);
  }

  @Test
  @DisplayName("Deve criar novo item de menu com tamanhos e grupos de complementos")
  void shouldCreateMenuItemWithSizesAndComplements() {
    MenuItemSizeRequestDTO sizeDTO = new MenuItemSizeRequestDTO("Duplo", new BigDecimal("35.00"), 20, true);
    ComplementItemRequestDTO compDTO = new ComplementItemRequestDTO("Bacon", new BigDecimal("5.00"), true);
    ComplementGroupRequestDTO groupDTO = new ComplementGroupRequestDTO("Adicionais", false, 0, 2, List.of(compDTO));

    MenuItemRequestDTO request = new MenuItemRequestDTO(
        "Smash Burger",
        "Pão brioche e carne",
        new BigDecimal("28.00"),
        true,
        List.of(sizeDTO),
        List.of(groupDTO)
    );

    when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
    when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
      MenuItem m = invocation.getArgument(0);
      m.setId(UUID.randomUUID());
      return m;
    });

    MenuItemResponseDTO response = menuService.create(restaurantId, request, "dono@foodrush.com");

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Smash Burger");
    assertThat(response.sizes()).hasSize(1);
    assertThat(response.sizes().get(0).name()).isEqualTo("Duplo");
    assertThat(response.complementGroups()).hasSize(1);
    assertThat(response.complementGroups().get(0).items()).hasSize(1);
    verify(menuItemRepository).save(any(MenuItem.class));
  }

  @Test
  @DisplayName("Deve alternar disponibilidade de um item do menu")
  void shouldToggleAvailability() {
    UUID itemId = UUID.randomUUID();
    MenuItem item = MenuItem.builder()
        .id(itemId)
        .restaurant(restaurant)
        .name("Fritas")
        .available(true)
        .build();

    when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    MenuItemResponseDTO response = menuService.toggleAvailability(itemId, "dono@foodrush.com");

    assertThat(response.available()).isFalse();
    verify(menuItemRepository).save(item);
  }

  @Test
  @DisplayName("Deve lançar AccessDeniedException quando outro restaurante tentar alterar o cardápio")
  void shouldThrowWhenNotOwner() {
    UUID itemId = UUID.randomUUID();
    MenuItem item = MenuItem.builder()
        .id(itemId)
        .restaurant(restaurant)
        .name("Fritas")
        .available(true)
        .build();

    when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> menuService.toggleAvailability(itemId, "invasor@foodrush.com"))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("não possui permissão");
  }

  @Test
  @DisplayName("Deve lançar exceção ao buscar restaurante inexistente")
  void shouldThrowWhenRestaurantNotFound() {
    when(restaurantRepository.existsById(restaurantId)).thenReturn(false);

    assertThatThrownBy(() -> menuService.findMenuByRestaurant(restaurantId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Restaurant not found");
  }
}

