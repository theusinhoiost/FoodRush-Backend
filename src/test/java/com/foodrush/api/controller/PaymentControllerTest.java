package com.foodrush.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.MercadoPagoWebhookRequestDTO;
import com.foodrush.api.dto.response.PaymentResponseDTO;
import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import com.foodrush.api.service.PaymentService;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private PaymentController paymentController;

  @Test
  @DisplayName("POST /payments/orders/{orderId}/pix should return 201 Created with payment details")
  void shouldCreatePixPaymentSuccessfully() {
    UUID orderId = UUID.randomUUID();
    Principal principal = () -> "cliente@email.com";

    PaymentResponseDTO responseDTO = new PaymentResponseDTO(
        UUID.randomUUID(),
        orderId,
        "123456",
        PaymentMethod.PIX,
        PaymentStatus.PENDING,
        new BigDecimal("59.90"),
        "000201...",
        "base64...",
        "https://ticket.url",
        OffsetDateTime.now()
    );

    when(paymentService.createPixPayment(orderId, "cliente@email.com")).thenReturn(responseDTO);

    ResponseEntity<PaymentResponseDTO> response = paymentController.createPixPayment(orderId, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().orderId()).isEqualTo(orderId);
    assertThat(response.getBody().paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.getBody().qrCode()).isEqualTo("000201...");
  }

  @Test
  @DisplayName("GET /payments/orders/{orderId} should return 200 OK with payment details")
  void shouldGetPaymentByOrderId() {
    UUID orderId = UUID.randomUUID();
    Principal principal = () -> "cliente@email.com";
    Authentication authentication = mock(Authentication.class);
    doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")))
        .when(authentication).getAuthorities();

    PaymentResponseDTO responseDTO = new PaymentResponseDTO(
        UUID.randomUUID(),
        orderId,
        "123456",
        PaymentMethod.PIX,
        PaymentStatus.APPROVED,
        new BigDecimal("59.90"),
        "000201...",
        "base64...",
        "https://ticket.url",
        OffsetDateTime.now()
    );

    when(paymentService.getPaymentByOrderId(orderId, "cliente@email.com", false)).thenReturn(responseDTO);

    ResponseEntity<PaymentResponseDTO> response = paymentController.getPaymentByOrderId(orderId, principal, authentication);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(PaymentStatus.APPROVED);
  }

  @Test
  @DisplayName("POST /payments/webhook should return 200 OK after delegating to PaymentService")
  void shouldHandleWebhookSuccessfully() {
    MercadoPagoWebhookRequestDTO payload = new MercadoPagoWebhookRequestDTO(
        "payment.updated",
        "payment",
        new MercadoPagoWebhookRequestDTO.WebhookData("987654"),
        "2026-11-13T23:00:00Z",
        123L,
        true,
        456L
    );

    ResponseEntity<Void> response = paymentController.handleWebhook(payload, "payment", "987654", "987654");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(paymentService, times(1)).processWebhookNotification(eq("payment"), eq("987654"), eq(payload));
  }
}
