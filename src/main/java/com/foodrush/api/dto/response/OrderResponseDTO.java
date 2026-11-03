package com.foodrush.api.dto.response;

import com.foodrush.api.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed order representation returned by Order endpoints")
public record OrderResponseDTO(
    @Schema(description = "Unique UUID of the order", example = "99999999-9999-9999-9999-999999999999")
    UUID id,

    @Schema(description = "UUID of the customer who placed the order")
    UUID customerId,

    @Schema(description = "Customer full name", example = "Matheus Silva")
    String customerName,

    @Schema(description = "UUID of the fulfilling restaurant")
    UUID restaurantId,

    @Schema(description = "Restaurant business name", example = "Burger Rush Jardins")
    String restaurantName,

    @Schema(description = "Current lifecycle status of the order", example = "CREATED")
    OrderStatus status,

    @Schema(description = "Subtotal of all ordered items and complements", example = "64.80")
    BigDecimal subtotal,

    @Schema(description = "Delivery fee charged by the restaurant", example = "7.50")
    BigDecimal deliveryFee,

    @Schema(description = "Final total amount for the order (subtotal + deliveryFee)", example = "72.30")
    BigDecimal total,

    @Schema(description = "Delivery destination address text", example = "Rua Augusta, 100, Apto 42 - Consolação, São Paulo - SP")
    String deliveryAddress,

    @Schema(description = "Client idempotency key used during creation", example = "foodrush-ord-key-991283")
    String idempotencyKey,

    @Schema(description = "Timestamp when the order was placed")
    OffsetDateTime createdAt,

    @Schema(description = "Timestamp when the order was last updated")
    OffsetDateTime updatedAt,

    @Schema(description = "Detailed list of all items and options included in the order")
    List<OrderItemResponseDTO> items
) {}

