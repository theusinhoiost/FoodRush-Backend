package com.foodrush.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.request.RestaurantPatchRequestDTO;
import com.foodrush.api.dto.request.RestaurantRequestDTO;
import com.foodrush.api.dto.response.AddressResponseDTO;
import com.foodrush.api.dto.response.RestaurantResponseDTO;
import com.foodrush.api.service.DeliveryCalculationService;
import com.foodrush.api.service.RestaurantService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

  @Mock
  private RestaurantService restaurantService;

  @Mock
  private DeliveryCalculationService deliveryCalculationService;

  @InjectMocks
  private RestaurantController restaurantController;

  private RestaurantResponseDTO createSampleResponse(UUID id) {
    AddressResponseDTO addressDTO = new AddressResponseDTO(
        UUID.randomUUID(), "01001-000", "Praça da Sé", "100", "Loja 1", "Sé", "São Paulo", "SP",
        new BigDecimal("-23.550520"), new BigDecimal("-46.633309")
    );
    return new RestaurantResponseDTO(
        id, UUID.randomUUID(), "Burger King", "48555407000133",
        new BigDecimal("5.00"), new BigDecimal("-23.550520"), new BigDecimal("-46.633309"),
        addressDTO, false
    );
  }

  @Test
  @DisplayName("POST /restaurants deve repassar email do Principal e retornar 201 Created")
  void shouldCreateRestaurantPassingPrincipalEmail() {
    UUID ownerId = UUID.randomUUID();
    AddressRequestDTO addressDTO = new AddressRequestDTO("01001-000", "100", "Loja 1");
    RestaurantRequestDTO request = new RestaurantRequestDTO(
        ownerId, "Burger King", "48555407000133", "11999998888", new BigDecimal("5.00"), addressDTO
    );
    RestaurantResponseDTO responseDTO = createSampleResponse(UUID.randomUUID());

    Principal principal = () -> "dono@foodrush.com";
    when(restaurantService.create(eq(request), eq("dono@foodrush.com"))).thenReturn(responseDTO);

    ResponseEntity<RestaurantResponseDTO> response = restaurantController.create(request, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo(responseDTO);
    verify(restaurantService).create(request, "dono@foodrush.com");
  }

  @Test
  @DisplayName("PUT /restaurants/{id} deve repassar email do Principal e retornar 200 OK")
  void shouldUpdateRestaurantPassingPrincipalEmail() {
    UUID restaurantId = UUID.randomUUID();
    AddressRequestDTO addressDTO = new AddressRequestDTO("01001-000", "100", "Loja 1");
    RestaurantRequestDTO request = new RestaurantRequestDTO(
        UUID.randomUUID(), "Burger King", "48555407000133", "11999998888", new BigDecimal("5.00"), addressDTO
    );
    RestaurantResponseDTO responseDTO = createSampleResponse(restaurantId);

    Principal principal = () -> "dono@foodrush.com";
    when(restaurantService.update(eq(restaurantId), eq(request), eq("dono@foodrush.com"))).thenReturn(responseDTO);

    ResponseEntity<RestaurantResponseDTO> response = restaurantController.update(restaurantId, request, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(responseDTO);
    verify(restaurantService).update(restaurantId, request, "dono@foodrush.com");
  }

  @Test
  @DisplayName("PATCH /restaurants/{id} deve repassar email do Principal e retornar 200 OK")
  void shouldPatchRestaurantPassingPrincipalEmail() {
    UUID restaurantId = UUID.randomUUID();
    RestaurantPatchRequestDTO request = new RestaurantPatchRequestDTO(
        null, "Novo Nome", null, null, null, null, null
    );
    RestaurantResponseDTO responseDTO = createSampleResponse(restaurantId);

    Principal principal = () -> "dono@foodrush.com";
    when(restaurantService.patch(eq(restaurantId), eq(request), eq("dono@foodrush.com"))).thenReturn(responseDTO);

    ResponseEntity<RestaurantResponseDTO> response = restaurantController.patch(restaurantId, request, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(responseDTO);
    verify(restaurantService).patch(restaurantId, request, "dono@foodrush.com");
  }

  @Test
  @DisplayName("PATCH /restaurants/{id}/toggle-open deve repassar email do Principal e retornar 200 OK")
  void shouldToggleOpenStatusPassingPrincipalEmail() {
    UUID restaurantId = UUID.randomUUID();
    RestaurantResponseDTO responseDTO = createSampleResponse(restaurantId);

    Principal principal = () -> "dono@foodrush.com";
    when(restaurantService.toggleOpenStatus(eq(restaurantId), eq("dono@foodrush.com"))).thenReturn(responseDTO);

    ResponseEntity<RestaurantResponseDTO> response = restaurantController.toggleOpenStatus(restaurantId, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(responseDTO);
    verify(restaurantService).toggleOpenStatus(restaurantId, "dono@foodrush.com");
  }

  @Test
  @DisplayName("GET /restaurants deve retornar lista de restaurantes com 200 OK")
  void shouldFindAllRestaurants() {
    RestaurantResponseDTO responseDTO = createSampleResponse(UUID.randomUUID());
    when(restaurantService.findAll()).thenReturn(List.of(responseDTO));

    ResponseEntity<List<RestaurantResponseDTO>> response = restaurantController.findAll();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    verify(restaurantService).findAll();
  }
}
