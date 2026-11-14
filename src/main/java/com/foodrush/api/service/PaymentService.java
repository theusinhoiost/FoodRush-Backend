package com.foodrush.api.service;

import com.foodrush.api.dto.request.CardPaymentRequestDTO;
import com.foodrush.api.dto.request.MercadoPagoWebhookRequestDTO;
import com.foodrush.api.dto.response.PaymentResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Order;
import com.foodrush.api.model.Payment;
import com.foodrush.api.repository.OrderRepository;
import com.foodrush.api.repository.PaymentRepository;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final OrderService orderService;
  private final PaymentClient paymentClient;

  @Value("${mercadopago.notification-url:}")
  private String notificationUrl;

  @Value("${mercadopago.public-key:}")
  private String publicKey;

  public String getPublicKey() {
    return publicKey;
  }

  @Transactional
  public PaymentResponseDTO createPixPayment(UUID orderId, String authenticatedCustomerEmail) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("Order not found with ID: " + orderId));

    if (!order.getCustomer().getEmail().equalsIgnoreCase(authenticatedCustomerEmail)) {
      throw new AccessDeniedException("User is not authorized to create payment for this order");
    }

    if (order.getStatus() != OrderStatus.CREATED) {
      throw new BusinessException("Cannot initiate payment: Order is in status '" + order.getStatus() + "'");
    }

    // Reuse existing pending payment with QR code if already generated
    Optional<Payment> existingPending = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
    if (existingPending.isPresent() && existingPending.get().getStatus() == PaymentStatus.PENDING) {
      log.info("Returning existing pending Pix payment for order ID: {}", orderId);
      return toDTO(existingPending.get());
    }

    String customerName = order.getCustomer().getName() != null ? order.getCustomer().getName().trim() : "Cliente";
    String[] nameParts = customerName.split("\\s+", 2);
    String firstName = nameParts[0];
    String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

    PaymentPayerRequest.PaymentPayerRequestBuilder payerBuilder = PaymentPayerRequest.builder()
        .email(order.getCustomer().getEmail())
        .firstName(firstName)
        .lastName(lastName);

    if (order.getCustomer().getCpf() != null && !order.getCustomer().getCpf().isBlank()) {
      String cleanCpf = order.getCustomer().getCpf().replaceAll("\\D", "");
      payerBuilder.identification(
          IdentificationRequest.builder()
              .type("CPF")
              .number(cleanCpf)
              .build()
      );
    }

    PaymentCreateRequest.PaymentCreateRequestBuilder requestBuilder = PaymentCreateRequest.builder()
        .transactionAmount(order.getTotal())
        .description("FoodRush Pedido #" + order.getId())
        .paymentMethodId("pix")
        .dateOfExpiration(OffsetDateTime.now().plusMinutes(30))
        .externalReference(order.getId().toString())
        .payer(payerBuilder.build());

    if (notificationUrl != null && !notificationUrl.isBlank()) {
      requestBuilder.notificationUrl(notificationUrl);
    }

    com.mercadopago.resources.payment.Payment mpPayment;
    try {
      mpPayment = paymentClient.create(requestBuilder.build());
    } catch (MPApiException e) {
      String mpError = e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage();
      log.error("Mercado Pago API error creating Pix payment for order {}: {} (Status {})", orderId, mpError, e.getStatusCode(), e);
      throw new BusinessException("Mercado Pago API error (" + e.getStatusCode() + "): " + mpError);
    } catch (MPException e) {
      log.error("Mercado Pago SDK error creating Pix payment for order {}: {}", orderId, e.getMessage(), e);
      throw new BusinessException("Failed to generate Pix payment with Mercado Pago: " + e.getMessage());
    }

    String qrCode = null;
    String qrCodeBase64 = null;
    String ticketUrl = null;
    if (mpPayment.getPointOfInteraction() != null
        && mpPayment.getPointOfInteraction().getTransactionData() != null) {
      var tx = mpPayment.getPointOfInteraction().getTransactionData();
      qrCode = tx.getQrCode();
      qrCodeBase64 = tx.getQrCodeBase64();
      ticketUrl = tx.getTicketUrl();
    }

    PaymentStatus paymentStatus = mapMpStatus(mpPayment.getStatus());

    Payment payment = Payment.builder()
        .order(order)
        .externalId(mpPayment.getId() != null ? mpPayment.getId().toString() : null)
        .paymentMethod(PaymentMethod.PIX)
        .status(paymentStatus)
        .amount(order.getTotal())
        .qrCode(qrCode)
        .qrCodeBase64(qrCodeBase64)
        .ticketUrl(ticketUrl)
        .build();

    Payment savedPayment = paymentRepository.save(payment);
    log.info("Pix payment generated successfully with ID {} (MP ID: {}) for Order {}",
        savedPayment.getId(), savedPayment.getExternalId(), order.getId());

    if (paymentStatus == PaymentStatus.APPROVED) {
      orderService.updateStatus(order.getId(), OrderStatus.PAID);
    }

    return toDTO(savedPayment);
  }

  @Transactional
  public PaymentResponseDTO createCardPayment(UUID orderId, String authenticatedCustomerEmail, CardPaymentRequestDTO request) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("Order not found with ID: " + orderId));

    if (!order.getCustomer().getEmail().equalsIgnoreCase(authenticatedCustomerEmail)) {
      throw new AccessDeniedException("User is not authorized to create payment for this order");
    }

    if (order.getStatus() != OrderStatus.CREATED) {
      throw new BusinessException("Cannot initiate payment: Order is in status '" + order.getStatus() + "'");
    }

    String customerName = order.getCustomer().getName() != null ? order.getCustomer().getName().trim() : "Cliente";
    String[] nameParts = customerName.split("\\s+", 2);
    String firstName = nameParts[0];
    String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

    PaymentPayerRequest.PaymentPayerRequestBuilder payerBuilder = PaymentPayerRequest.builder()
        .email(order.getCustomer().getEmail())
        .firstName(firstName)
        .lastName(lastName);

    if (order.getCustomer().getCpf() != null && !order.getCustomer().getCpf().isBlank()) {
      String cleanCpf = order.getCustomer().getCpf().replaceAll("\\D", "");
      payerBuilder.identification(
          IdentificationRequest.builder()
              .type("CPF")
              .number(cleanCpf)
              .build()
      );
    }

    PaymentCreateRequest.PaymentCreateRequestBuilder requestBuilder = PaymentCreateRequest.builder()
        .transactionAmount(order.getTotal())
        .token(request.token())
        .description("FoodRush Pedido #" + order.getId())
        .installments(request.installments())
        .paymentMethodId(request.paymentMethodId())
        .externalReference(order.getId().toString())
        .payer(payerBuilder.build());

    if (request.issuerId() != null && !request.issuerId().isBlank()) {
      requestBuilder.issuerId(request.issuerId());
    }

    if (notificationUrl != null && !notificationUrl.isBlank()) {
      requestBuilder.notificationUrl(notificationUrl);
    }

    com.mercadopago.resources.payment.Payment mpPayment;
    try {
      mpPayment = paymentClient.create(requestBuilder.build());
    } catch (MPApiException e) {
      String mpError = e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage();
      log.error("Mercado Pago API error creating Card payment for order {}: {} (Status {})", orderId, mpError, e.getStatusCode(), e);
      throw new BusinessException("Mercado Pago API error (" + e.getStatusCode() + "): " + mpError);
    } catch (MPException e) {
      log.error("Mercado Pago SDK error creating Card payment for order {}: {}", orderId, e.getMessage(), e);
      throw new BusinessException("Failed to process Card payment with Mercado Pago: " + e.getMessage());
    }

    PaymentStatus paymentStatus = mapMpStatus(mpPayment.getStatus());

    String lastFour = null;
    String brand = request.paymentMethodId();
    if (mpPayment.getCard() != null) {
      lastFour = mpPayment.getCard().getLastFourDigits();
    }

    Payment payment = Payment.builder()
        .order(order)
        .externalId(mpPayment.getId() != null ? mpPayment.getId().toString() : null)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .status(paymentStatus)
        .amount(order.getTotal())
        .installments(request.installments())
        .cardLastFour(lastFour)
        .cardBrand(brand)
        .build();

    Payment savedPayment = paymentRepository.save(payment);
    log.info("Credit card payment processed with ID {} (MP ID: {}, Status: {}) for Order {}",
        savedPayment.getId(), savedPayment.getExternalId(), paymentStatus, order.getId());

    if (paymentStatus == PaymentStatus.APPROVED) {
      orderService.updateStatus(order.getId(), OrderStatus.PAID);
    }

    return toDTO(savedPayment);
  }

  @Transactional(readOnly = true)
  public PaymentResponseDTO getPaymentByOrderId(UUID orderId, String authenticatedUserEmail, boolean isAdmin) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException("Order not found with ID: " + orderId));

    boolean isCustomer = order.getCustomer().getEmail().equalsIgnoreCase(authenticatedUserEmail);
    boolean isRestaurantOwner = order.getRestaurant().getOwner() != null
        && order.getRestaurant().getOwner().getEmail().equalsIgnoreCase(authenticatedUserEmail);

    if (!isCustomer && !isRestaurantOwner && !isAdmin) {
      throw new AccessDeniedException("User is not authorized to view payments for this order");
    }

    Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
        .orElseThrow(() -> new BusinessException("No payment found for order: " + orderId));

    return toDTO(payment);
  }

  @Transactional
  public boolean processWebhookNotification(String topic, String resourceId, MercadoPagoWebhookRequestDTO payload) {
    String paymentIdStr = null;

    if (payload != null && payload.data() != null && payload.data().id() != null) {
      paymentIdStr = payload.data().id();
    } else if (resourceId != null && !resourceId.isBlank()) {
      paymentIdStr = resourceId;
    }

    if (paymentIdStr == null || paymentIdStr.isBlank()) {
      log.debug("Mercado Pago webhook received without payment identifier. Ignoring.");
      return false;
    }

    log.info("Processing Mercado Pago webhook for payment external ID: {}", paymentIdStr);

    com.mercadopago.resources.payment.Payment mpPayment;
    try {
      mpPayment = paymentClient.get(Long.parseLong(paymentIdStr));
    } catch (Exception e) {
      log.error("Failed to query Mercado Pago payment details for ID {}: {}", paymentIdStr, e.getMessage());
      return false;
    }

    String externalId = mpPayment.getId() != null ? mpPayment.getId().toString() : paymentIdStr;

    Payment payment = paymentRepository.findByExternalIdWithOrder(externalId)
        .orElseGet(() -> {
          if (mpPayment.getExternalReference() != null) {
            try {
              UUID orderUuid = UUID.fromString(mpPayment.getExternalReference());
              return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderUuid).orElse(null);
            } catch (IllegalArgumentException ignored) {}
          }
          return null;
        });

    if (payment == null) {
      log.warn("Payment record not found for external payment ID {}", externalId);
      return false;
    }

    PaymentStatus newStatus = mapMpStatus(mpPayment.getStatus());
    payment.setStatus(newStatus);
    payment.setExternalId(externalId);
    paymentRepository.save(payment);

    if (newStatus == PaymentStatus.APPROVED) {
      if (payment.getOrder().getStatus() == OrderStatus.CREATED) {
        log.info("Payment {} approved. Transitioning Order {} to PAID and triggering events.",
            payment.getId(), payment.getOrder().getId());
        orderService.updateStatus(payment.getOrder().getId(), OrderStatus.PAID);
      }
    } else if (newStatus == PaymentStatus.REJECTED || newStatus == PaymentStatus.CANCELLED) {
      log.warn("Payment {} status updated to {} for Order {}", payment.getId(), newStatus, payment.getOrder().getId());
    }

    return true;
  }

  private PaymentStatus mapMpStatus(String mpStatus) {
    if (mpStatus == null) {
      return PaymentStatus.PENDING;
    }
    return switch (mpStatus.toLowerCase()) {
      case "approved" -> PaymentStatus.APPROVED;
      case "rejected" -> PaymentStatus.REJECTED;
      case "cancelled" -> PaymentStatus.CANCELLED;
      case "refunded", "charged_back" -> PaymentStatus.REFUNDED;
      default -> PaymentStatus.PENDING;
    };
  }

  private PaymentResponseDTO toDTO(Payment payment) {
    return new PaymentResponseDTO(
        payment.getId(),
        payment.getOrder().getId(),
        payment.getExternalId(),
        payment.getPaymentMethod(),
        payment.getStatus(),
        payment.getAmount(),
        payment.getInstallments(),
        payment.getCardLastFour(),
        payment.getCardBrand(),
        payment.getQrCode(),
        payment.getQrCodeBase64(),
        payment.getTicketUrl(),
        payment.getCreatedAt()
    );
  }
}
