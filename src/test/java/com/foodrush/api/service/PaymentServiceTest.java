package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.MercadoPagoWebhookRequestDTO;
import com.foodrush.api.dto.response.PaymentResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Order;
import com.foodrush.api.model.Payment;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.OrderRepository;
import com.foodrush.api.repository.PaymentRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.resources.payment.PaymentPointOfInteraction;
import com.mercadopago.resources.payment.PaymentTransactionData;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderService orderService;

  @Mock
  private PaymentClient paymentClient;

  @InjectMocks
  private PaymentService paymentService;

  private User customer;
  private User restaurantOwner;
  private Restaurant restaurant;
  private Order order;

  @BeforeEach
  void setUp() {
    customer = User.builder()
        .id(UUID.randomUUID())
        .name("João Silva")
        .email("joao.silva@email.com")
        .role(RoleType.CLIENT)
        .cpf("12345678900")
        .build();

    restaurantOwner = User.builder()
        .id(UUID.randomUUID())
        .name("Maria Restaurante")
        .email("maria@restaurante.com")
        .role(RoleType.RESTAURANT)
        .build();

    restaurant = Restaurant.builder()
        .id(UUID.randomUUID())
        .tradeName("Pizzaria Bella")
        .owner(restaurantOwner)
        .build();

    order = Order.builder()
        .id(UUID.randomUUID())
        .customer(customer)
        .restaurant(restaurant)
        .status(OrderStatus.CREATED)
        .total(new BigDecimal("49.90"))
        .createdAt(OffsetDateTime.now())
        .build();
  }

  @Test
  @DisplayName("Should create Pix payment successfully with Mercado Pago SDK")
  void shouldCreatePixPaymentSuccessfully() throws Exception {
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.empty());

    com.mercadopago.resources.payment.Payment mpPayment = mock(com.mercadopago.resources.payment.Payment.class);
    PaymentPointOfInteraction poi = mock(PaymentPointOfInteraction.class);
    PaymentTransactionData txData = mock(PaymentTransactionData.class);

    when(mpPayment.getId()).thenReturn(123456789L);
    when(mpPayment.getStatus()).thenReturn("pending");
    when(mpPayment.getPointOfInteraction()).thenReturn(poi);
    when(poi.getTransactionData()).thenReturn(txData);
    when(txData.getQrCode()).thenReturn("00020126580014br.gov.bcb.pix...");
    when(txData.getQrCodeBase64()).thenReturn("iVBORw0KGgoAAAANSUhEUgAA...");
    when(txData.getTicketUrl()).thenReturn("https://mercadopago.com/ticket/123456789");

    when(paymentClient.create(any(PaymentCreateRequest.class))).thenReturn(mpPayment);

    Payment savedEntity = Payment.builder()
        .id(UUID.randomUUID())
        .order(order)
        .externalId("123456789")
        .paymentMethod(PaymentMethod.PIX)
        .status(PaymentStatus.PENDING)
        .amount(order.getTotal())
        .qrCode("00020126580014br.gov.bcb.pix...")
        .qrCodeBase64("iVBORw0KGgoAAAANSUhEUgAA...")
        .ticketUrl("https://mercadopago.com/ticket/123456789")
        .createdAt(OffsetDateTime.now())
        .build();

    when(paymentRepository.save(any(Payment.class))).thenReturn(savedEntity);

    PaymentResponseDTO response = paymentService.createPixPayment(order.getId(), customer.getEmail());

    assertThat(response).isNotNull();
    assertThat(response.externalId()).isEqualTo("123456789");
    assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(response.qrCode()).isEqualTo("00020126580014br.gov.bcb.pix...");
    assertThat(response.qrCodeBase64()).isEqualTo("iVBORw0KGgoAAAANSUhEUgAA...");
    assertThat(response.amount()).isEqualTo(new BigDecimal("49.90"));

    verify(paymentRepository, times(1)).save(any(Payment.class));
  }

  @Test
  @DisplayName("Should return existing pending Pix payment if already generated")
  void shouldReturnExistingPendingPayment() {
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    Payment existing = Payment.builder()
        .id(UUID.randomUUID())
        .order(order)
        .externalId("987654321")
        .paymentMethod(PaymentMethod.PIX)
        .status(PaymentStatus.PENDING)
        .amount(order.getTotal())
        .qrCode("existing-pix-qr")
        .qrCodeBase64("existing-qr-base64")
        .createdAt(OffsetDateTime.now())
        .build();

    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()))
        .thenReturn(Optional.of(existing));

    PaymentResponseDTO response = paymentService.createPixPayment(order.getId(), customer.getEmail());

    assertThat(response).isNotNull();
    assertThat(response.externalId()).isEqualTo("987654321");
    assertThat(response.qrCode()).isEqualTo("existing-pix-qr");

    verifyNoInteractions(paymentClient);
  }

  @Test
  @DisplayName("Should throw AccessDeniedException when unauthenticated or different customer attempts to pay")
  void shouldThrowWhenCustomerIsNotOrderOwner() {
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> paymentService.createPixPayment(order.getId(), "outro.usuario@email.com"))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("User is not authorized to create payment for this order");
  }

  @Test
  @DisplayName("Should throw BusinessException when order is not in CREATED status")
  void shouldThrowWhenOrderStatusIsNotCreated() {
    order.setStatus(OrderStatus.PAID);
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> paymentService.createPixPayment(order.getId(), customer.getEmail()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Cannot initiate payment: Order is in status 'PAID'");
  }

  @Test
  @DisplayName("Should allow customer, restaurant owner, and admin to retrieve payment details")
  void shouldAllowAuthorizedRolesToGetPayment() {
    Payment payment = Payment.builder()
        .id(UUID.randomUUID())
        .order(order)
        .externalId("123")
        .paymentMethod(PaymentMethod.PIX)
        .status(PaymentStatus.PENDING)
        .amount(order.getTotal())
        .build();

    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.of(payment));

    // 1. Customer
    PaymentResponseDTO customerResult = paymentService.getPaymentByOrderId(order.getId(), customer.getEmail(), false);
    assertThat(customerResult).isNotNull();

    // 2. Restaurant Owner
    PaymentResponseDTO ownerResult = paymentService.getPaymentByOrderId(order.getId(), restaurantOwner.getEmail(), false);
    assertThat(ownerResult).isNotNull();

    // 3. Admin
    PaymentResponseDTO adminResult = paymentService.getPaymentByOrderId(order.getId(), "admin@foodrush.com", true);
    assertThat(adminResult).isNotNull();

    // 4. Unauthorized user
    assertThatThrownBy(() -> paymentService.getPaymentByOrderId(order.getId(), "hacker@email.com", false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Should process Mercado Pago webhook and transition Order to PAID when approved")
  void shouldProcessWebhookApprovedAndTransitionOrderToPaid() throws Exception {
    String paymentIdStr = "555123456";
    com.mercadopago.resources.payment.Payment mpPayment = mock(com.mercadopago.resources.payment.Payment.class);
    when(mpPayment.getId()).thenReturn(555123456L);
    when(mpPayment.getStatus()).thenReturn("approved");
    when(paymentClient.get(555123456L)).thenReturn(mpPayment);

    Payment payment = Payment.builder()
        .id(UUID.randomUUID())
        .order(order)
        .externalId(paymentIdStr)
        .paymentMethod(PaymentMethod.PIX)
        .status(PaymentStatus.PENDING)
        .amount(order.getTotal())
        .build();

    when(paymentRepository.findByExternalIdWithOrder(paymentIdStr)).thenReturn(Optional.of(payment));

    MercadoPagoWebhookRequestDTO payload = new MercadoPagoWebhookRequestDTO(
        "payment.updated",
        "payment",
        new MercadoPagoWebhookRequestDTO.WebhookData(paymentIdStr),
        "2026-11-13T23:00:00Z",
        555123456L,
        true,
        123456789L
    );

    boolean processed = paymentService.processWebhookNotification(null, paymentIdStr, payload);

    assertThat(processed).isTrue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    verify(paymentRepository, times(1)).save(payment);
    verify(orderService, times(1)).updateStatus(order.getId(), OrderStatus.PAID);
  }

  @Test
  @DisplayName("Should handle webhook for rejected payment without transitioning order to PAID")
  void shouldHandleWebhookRejectedPayment() throws Exception {
    String paymentIdStr = "999888777";
    com.mercadopago.resources.payment.Payment mpPayment = mock(com.mercadopago.resources.payment.Payment.class);
    when(mpPayment.getId()).thenReturn(999888777L);
    when(mpPayment.getStatus()).thenReturn("rejected");
    when(paymentClient.get(999888777L)).thenReturn(mpPayment);

    Payment payment = Payment.builder()
        .id(UUID.randomUUID())
        .order(order)
        .externalId(paymentIdStr)
        .paymentMethod(PaymentMethod.PIX)
        .status(PaymentStatus.PENDING)
        .amount(order.getTotal())
        .build();

    when(paymentRepository.findByExternalIdWithOrder(paymentIdStr)).thenReturn(Optional.of(payment));

    boolean processed = paymentService.processWebhookNotification("payment", paymentIdStr, null);

    assertThat(processed).isTrue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    verify(paymentRepository, times(1)).save(payment);
    verify(orderService, never()).updateStatus(any(), any());
  }
}
