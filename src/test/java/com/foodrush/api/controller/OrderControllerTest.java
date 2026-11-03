package com.foodrush.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodrush.api.dto.request.OrderCreateRequestDTO;
import com.foodrush.api.dto.request.OrderItemRequestDTO;
import com.foodrush.api.dto.request.OrderStatusUpdateRequestDTO;
import com.foodrush.api.dto.response.OrderResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.service.OrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class OrderControllerTest {

  @Mock
  private OrderService orderService;

  @InjectMocks
  private OrderController orderController;

  @Test
  @DisplayName("POST /orders deve retornar 201 Created quando pedido for criado com sucesso")
  void shouldCreateOrderSuccessfully() {
    UUID customerId = UUID.randomUUID();
    UUID restaurantId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();

    OrderCreateRequestDTO request = new OrderCreateRequestDTO(
        customerId,
        restaurantId,
        "Rua Oscar Freire, 500",
        "key-123",
        List.of(new OrderItemRequestDTO(UUID.randomUUID(), null, 1, null))
    );

    OrderResponseDTO mockResponse = new OrderResponseDTO(
        orderId,
        customerId,
        "Cliente Teste",
        restaurantId,
        "Pizzaria Rush",
        OrderStatus.CREATED,
        new BigDecimal("50.00"),
        new BigDecimal("10.00"),
        new BigDecimal("60.00"),
        "Rua Oscar Freire, 500",
        "key-123",
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        List.of()
    );

    when(orderService.create(any(OrderCreateRequestDTO.class))).thenReturn(mockResponse);

    ResponseEntity<OrderResponseDTO> response = orderController.create(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(orderId);
    assertThat(response.getBody().total()).isEqualByComparingTo("60.00");
    verify(orderService).create(request);
  }

  @Test
  @DisplayName("GET /orders/{id} deve retornar 200 OK com detalhes do pedido")
  void shouldFindOrderById() {
    UUID orderId = UUID.randomUUID();
    OrderResponseDTO mockResponse = new OrderResponseDTO(
        orderId,
        UUID.randomUUID(),
        "Cliente Teste",
        UUID.randomUUID(),
        "Pizzaria Rush",
        OrderStatus.PAID,
        new BigDecimal("50.00"),
        new BigDecimal("10.00"),
        new BigDecimal("60.00"),
        "Rua Oscar Freire, 500",
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        List.of()
    );

    when(orderService.findById(orderId)).thenReturn(mockResponse);

    ResponseEntity<OrderResponseDTO> response = orderController.findById(orderId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(mockResponse);
    verify(orderService).findById(orderId);
  }

  @Test
  @DisplayName("GET /orders/customer/{customerId} deve retornar 200 OK com histórico do cliente")
  void shouldFindOrdersByCustomerId() {
    UUID customerId = UUID.randomUUID();
    List<OrderResponseDTO> orders = List.of(
        new OrderResponseDTO(
            UUID.randomUUID(), customerId, "Cliente", UUID.randomUUID(), "Rest",
            OrderStatus.DELIVERED, new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("30.00"),
            "Endereço", null, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        )
    );

    when(orderService.findAllByCustomerId(customerId)).thenReturn(orders);

    ResponseEntity<List<OrderResponseDTO>> response = orderController.findByCustomer(customerId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    verify(orderService).findAllByCustomerId(customerId);
  }

  @Test
  @DisplayName("PATCH /orders/{id}/status deve retornar 200 OK após atualizar status")
  void shouldUpdateOrderStatus() {
    UUID orderId = UUID.randomUUID();
    OrderStatusUpdateRequestDTO request = new OrderStatusUpdateRequestDTO(OrderStatus.PREPARING);

    OrderResponseDTO updatedOrder = new OrderResponseDTO(
        orderId, UUID.randomUUID(), "Cliente", UUID.randomUUID(), "Rest",
        OrderStatus.PREPARING, new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("40.00"),
        "Endereço", null, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
    );

    when(orderService.updateStatus(eq(orderId), eq(OrderStatus.PREPARING))).thenReturn(updatedOrder);

    ResponseEntity<OrderResponseDTO> response = orderController.updateStatus(orderId, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().status()).isEqualTo(OrderStatus.PREPARING);
    verify(orderService).updateStatus(orderId, OrderStatus.PREPARING);
  }
}

