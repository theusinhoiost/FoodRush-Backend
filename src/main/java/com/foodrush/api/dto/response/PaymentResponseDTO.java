package com.foodrush.api.dto.response;

import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Payment details including Mercado Pago Pix QR code and transaction status")
public record PaymentResponseDTO(
  @Schema(description = "Payment unique identifier", example = "e5d0f436-ed01-45fd-960d-349ffd19e4d5")
  UUID id,

  @Schema(description = "Associated order identifier", example = "39c7f9f7-5ad8-406b-b522-814386ceceb7")
  UUID orderId,

  @Schema(description = "External Mercado Pago transaction identifier", example = "12345678901")
  String externalId,

  @Schema(description = "Payment method", example = "PIX")
  PaymentMethod paymentMethod,

  @Schema(description = "Payment current processing status", example = "PENDING")
  PaymentStatus status,

  @Schema(description = "Total transaction amount", example = "45.90")
  BigDecimal amount,

  @Schema(description = "Pix Copia e Cola code string for user payment", example = "00020126580014br.gov.bcb.pix...")
  String qrCode,

  @Schema(description = "Pix QR Code encoded as base64 image PNG", example = "iVBORw0KGgoAAAANSUhEUgAAA...")
  String qrCodeBase64,

  @Schema(description = "Ticket URL hosted by Mercado Pago", example = "https://www.mercadopago.com.br/payments/12345678901/ticket")
  String ticketUrl,

  @Schema(description = "Creation timestamp", example = "2026-11-13T23:30:00Z")
  OffsetDateTime createdAt
) {}
