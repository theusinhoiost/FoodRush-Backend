package com.foodrush.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodrush.api.dto.request.MenuItemRequestDTO;
import com.foodrush.api.dto.response.MenuItemResponseDTO;
import com.foodrush.api.service.MenuService;
import java.math.BigDecimal;
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
class MenuControllerTest {

  @Mock
  private MenuService menuService;

  @InjectMocks
  private MenuController menuController;

  @Test
  @DisplayName("GET /restaurants/{restaurantId}/menu deve retornar 200 OK com lista de itens")
  void shouldReturnRestaurantMenu() {
    UUID restaurantId = UUID.randomUUID();
    MenuItemResponseDTO item = new MenuItemResponseDTO(
        UUID.randomUUID(), restaurantId, "Burger", "Delicious", new BigDecimal("25.00"),
        true, List.of(), List.of()
    );

    when(menuService.findMenuByRestaurant(restaurantId)).thenReturn(List.of(item));

    ResponseEntity<List<MenuItemResponseDTO>> response = menuController.getRestaurantMenu(restaurantId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    verify(menuService).findMenuByRestaurant(restaurantId);
  }

  @Test
  @DisplayName("POST /restaurants/{restaurantId}/menu-items deve retornar 201 Created")
  void shouldCreateMenuItem() {
    UUID restaurantId = UUID.randomUUID();
    MenuItemRequestDTO request = new MenuItemRequestDTO(
        "Pizza", "Massa artesanal", new BigDecimal("45.00"), true, List.of(), List.of()
    );

    MenuItemResponseDTO created = new MenuItemResponseDTO(
        UUID.randomUUID(), restaurantId, "Pizza", "Massa artesanal", new BigDecimal("45.00"),
        true, List.of(), List.of()
    );

    when(menuService.create(eq(restaurantId), any(MenuItemRequestDTO.class), any())).thenReturn(created);

    ResponseEntity<MenuItemResponseDTO> response = menuController.createMenuItem(restaurantId, request, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo(created);
    verify(menuService).create(restaurantId, request, null);
  }

  @Test
  @DisplayName("DELETE /menu-items/{id} deve retornar 204 No Content")
  void shouldDeleteMenuItem() {
    UUID itemId = UUID.randomUUID();

    ResponseEntity<Void> response = menuController.deleteMenuItem(itemId, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(menuService).delete(itemId, null);
  }
}

