package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Payload to define a specific size variation of a menu item")
public record MenuItemSizeRequestDTO(
    @NotBlank(message = "Size name is mandatory")
    @Size(max = 50, message = "Size name must not exceed 50 characters")
    @Schema(description = "Size label", example = "Grande (300g)")
    String name,

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @Schema(description = "Price for this size option", example = "38.90")
    BigDecimal price,

    @NotNull(message = "Stock quantity is mandatory")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Schema(description = "Initial stock quantity available for sale", example = "25")
    Integer stockQuantity,

    @Schema(description = "Whether this size is currently available", defaultValue = "true")
    Boolean available
) {}

