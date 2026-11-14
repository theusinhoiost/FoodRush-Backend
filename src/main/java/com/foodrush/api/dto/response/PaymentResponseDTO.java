package com.foodrush.api.dto.response;

import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Payment details response")
public record PaymentResponseDTO(
  @Schema(description = "Internal payment unique ID", example = "c81d4e2e-bcf2-11e6-869b-7df92533c26b")
  UUID id,

  @Schema(description = "Associated order ID", example = "a11d4e2e-bcf2-11e6-869b-7df92533c26b")
  UUID orderId,

  @Schema(description = "Mercado Pago external payment ID", example = "12345678901")
  String externalId,

  @Schema(description = "Payment method used", example = "PIX")
  PaymentMethod paymentMethod,

  @Schema(description = "Current payment status", example = "PENDING")
  PaymentStatus status,

  @Schema(description = "Total transaction amount", example = "49.90")
  BigDecimal amount,

  @Schema(description = "Number of card installments", example = "1")
  Integer installments,

  @Schema(description = "Last four digits of credit card", example = "1234")
  String cardLastFour,

  @Schema(description = "Credit card brand name", example = "master")
  String cardBrand,

  @Schema(description = "Pix Copia e Cola payload", example = "00020126580014br.gov.bcb.pix...")
  String qrCode,

  @Schema(description = "Base64 encoded QR Code image PNG", example = "iVBORw0KGgoAAAANSUhEUgAA...")
  String qrCodeBase64,

  @Schema(description = "Mercado Pago ticket payment URL")
  String ticketUrl,

  @Schema(description = "Payment creation timestamp")
  OffsetDateTime createdAt
) {}
