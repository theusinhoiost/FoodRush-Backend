package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Size variation detail response")
public record MenuItemSizeDetailDTO(
    @Schema(description = "Unique UUID of the size option")
    UUID id,

    @Schema(description = "Size label", example = "Grande (300g)")
    String name,

    @Schema(description = "Size price", example = "38.90")
    BigDecimal price,

    @Schema(description = "Available units in inventory", example = "25")
    Integer stockQuantity,

    @Schema(description = "Availability status")
    Boolean available
) implements Serializable {}


