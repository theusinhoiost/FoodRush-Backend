package com.foodrush.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full menu item response including sizes, inventory, and complement groups")
public record MenuItemResponseDTO(
    @Schema(description = "Unique UUID of the menu item")
    UUID id,

    @Schema(description = "UUID of the owning restaurant")
    UUID restaurantId,

    @Schema(description = "Dish or item name", example = "Hambúrguer Smash Rush")
    String name,

    @Schema(description = "Item description", example = "Pão brioche, 2x smash burger 90g e queijo cheddar")
    String description,

    @Schema(description = "Base price of the dish", example = "29.90")
    BigDecimal basePrice,

    @Schema(description = "Availability status")
    Boolean available,

    @Schema(description = "Available size options")
    List<MenuItemSizeDetailDTO> sizes,

    @Schema(description = "Available complement and customization groups")
    List<ComplementGroupDetailDTO> complementGroups
) implements Serializable {}


