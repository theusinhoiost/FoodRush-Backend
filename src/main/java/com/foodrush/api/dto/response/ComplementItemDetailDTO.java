package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Complement option item response")
public record ComplementItemDetailDTO(
    @Schema(description = "Unique UUID of the complement item")
    UUID id,

    @Schema(description = "Complement name", example = "Bacon Extra Crocante")
    String name,

    @Schema(description = "Additional price", example = "4.50")
    BigDecimal price,

    @Schema(description = "Availability status")
    Boolean available
) implements Serializable {}


