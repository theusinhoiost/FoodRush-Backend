package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Schema(description = "Complement or add-on group response")
public record ComplementGroupDetailDTO(
    @Schema(description = "Unique UUID of the complement group")
    UUID id,

    @Schema(description = "Group name", example = "Escolha seu Molho")
    String name,

    @Schema(description = "Whether selection is mandatory")
    Boolean required,

    @Schema(description = "Minimum items required")
    Integer minQuantity,

    @Schema(description = "Maximum items allowed")
    Integer maxQuantity,

    @Schema(description = "Items belonging to this group")
    List<ComplementItemDetailDTO> items
) implements Serializable {}


