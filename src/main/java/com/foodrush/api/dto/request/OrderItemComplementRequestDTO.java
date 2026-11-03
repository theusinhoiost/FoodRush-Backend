package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Complement or add-on item selection for a specific order item")
public record OrderItemComplementRequestDTO(
    @NotNull(message = "complementItemId is mandatory")
    @Schema(description = "Unique UUID of the complement item", example = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
    UUID complementItemId,

    @Schema(description = "Quantity of this complement item (default 1)", example = "1")
    Integer quantity
) {}

