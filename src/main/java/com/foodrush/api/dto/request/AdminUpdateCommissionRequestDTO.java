package com.foodrush.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminUpdateCommissionRequestDTO(
    @NotNull(message = "Commission rate cannot be null")
    @DecimalMin(value = "0.00", message = "Commission rate must be at least 0.00%")
    @DecimalMax(value = "100.00", message = "Commission rate cannot exceed 100.00%")
    BigDecimal commissionRate
) {}
