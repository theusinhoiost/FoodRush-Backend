package com.foodrush.api.dto.request;

import com.foodrush.api.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to transition an existing order to a new lifecycle status")
public record OrderStatusUpdateRequestDTO(
    @NotNull(message = "status is mandatory")
    @Schema(description = "Target status (CREATED, PAID, PREPARING, READY, IN_ROUTE, DELIVERED, CANCELED)", example = "PREPARING")
    OrderStatus status
) {}

