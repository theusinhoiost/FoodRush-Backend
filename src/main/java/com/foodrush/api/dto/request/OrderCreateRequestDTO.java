package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload to create a new delivery order")
public record OrderCreateRequestDTO(
    @NotNull(message = "customerId is mandatory")
    @Schema(description = "UUID of the customer placing the order", example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    UUID customerId,

    @NotNull(message = "restaurantId is mandatory")
    @Schema(description = "UUID of the restaurant fulfilling the order", example = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    UUID restaurantId,

    @Schema(description = "Delivery address string or full street details", example = "Rua Augusta, 100, Apto 42 - Consolação, São Paulo - SP")
    String deliveryAddress,

    @Schema(description = "Client-supplied idempotency key to prevent accidental duplicate orders", example = "foodrush-ord-key-991283")
    String idempotencyKey,

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    @Schema(description = "List of items contained in this order")
    List<OrderItemRequestDTO> items
) {}

