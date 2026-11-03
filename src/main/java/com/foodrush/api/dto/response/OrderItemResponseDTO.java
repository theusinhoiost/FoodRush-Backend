package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Details of an individual item in an order")
public record OrderItemResponseDTO(
    @Schema(description = "Unique UUID of the order item")
    UUID id,

    @Schema(description = "UUID of the original menu item entity")
    UUID menuItemId,

    @Schema(description = "Name of the menu item", example = "X-Bacon Artesanal")
    String menuItemName,

    @Schema(description = "Optional UUID of the chosen size")
    UUID sizeId,

    @Schema(description = "Optional label of the chosen size", example = "Duplo 300g")
    String sizeName,

    @Schema(description = "Quantity ordered", example = "2")
    Integer quantity,

    @Schema(description = "Unit price of the base item or selected size", example = "28.90")
    BigDecimal unitPrice,

    @Schema(description = "Total price calculated for this item and its complements", example = "64.80")
    BigDecimal totalPrice,

    @Schema(description = "Complements or add-ons included in this item")
    List<OrderItemComplementResponseDTO> complements
) {}

