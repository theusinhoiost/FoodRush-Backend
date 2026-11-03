package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "Individual item selection within an order creation payload")
public record OrderItemRequestDTO(
    @NotNull(message = "menuItemId is mandatory")
    @Schema(description = "Unique UUID of the menu item", example = "cccccccc-cccc-cccc-cccc-cccccccccccc")
    UUID menuItemId,

    @Schema(description = "Optional UUID of the specific size (e.g., Small, Medium, Large)", example = "dddddddd-dddd-dddd-dddd-dddddddddddd")
    UUID sizeId,

    @NotNull(message = "quantity is mandatory")
    @Schema(description = "Quantity of this item (must be at least 1)", example = "2")
    Integer quantity,

    @Schema(description = "Optional list of complement selections for this item")
    List<OrderItemComplementRequestDTO> complements
) {}

