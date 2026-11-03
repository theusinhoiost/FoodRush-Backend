package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Payload to create an add-on or complement item within a group")
public record ComplementItemRequestDTO(
    @NotBlank(message = "Complement item name is mandatory")
    @Size(max = 80, message = "Name must not exceed 80 characters")
    @Schema(description = "Name of the complement option", example = "Bacon Extra Crocante")
    String name,

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    @Schema(description = "Additional price charged for this complement", example = "4.50")
    BigDecimal price,

    @Schema(description = "Whether this complement is available for selection", defaultValue = "true")
    Boolean available
) {}

