package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Delivery fee and distance estimation quotation")
public record DeliveryFeeEstimateResponseDTO(
    @Schema(description = "UUID of the restaurant providing delivery")
    UUID restaurantId,

    @Schema(description = "Restaurant commercial name", example = "Pizzaria Rush")
    String restaurantName,

    @Schema(description = "Calculated distance in kilometers between customer and restaurant", example = "3.45")
    Double distanceKm,

    @Schema(description = "Base delivery fee charged by the restaurant", example = "5.00")
    BigDecimal baseFee,

    @Schema(description = "Additional fee charged per kilometer", example = "1.50")
    BigDecimal feePerKm,

    @Schema(description = "Total calculated delivery fee", example = "10.18")
    BigDecimal totalDeliveryFee,

    @Schema(description = "Estimated delivery time in minutes", example = "35")
    Integer estimatedMinutes
) {}

