package com.foodrush.api.dto.response;

import java.math.BigDecimal;

public record AdminMetricsResponseDTO(
    long totalRestaurants,
    long connectedRestaurants,
    long totalUsers,
    long totalOrders,
    BigDecimal totalGrossVolume,
    BigDecimal totalPlatformFees
) {}
