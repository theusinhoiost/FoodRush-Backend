package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Complement item attached to an ordered item")
public record OrderItemComplementResponseDTO(
    @Schema(description = "Unique UUID of the complement snapshot record")
    UUID id,

    @Schema(description = "UUID of the original complement item entity")
    UUID complementItemId,

    @Schema(description = "Name of the complement at time of ordering", example = "Molho Especial de Alho")
    String name,

    @Schema(description = "Unit price of the complement at purchase time", example = "3.50")
    BigDecimal price,

    @Schema(description = "Quantity of this complement chosen", example = "1")
    Integer quantity
) {}

