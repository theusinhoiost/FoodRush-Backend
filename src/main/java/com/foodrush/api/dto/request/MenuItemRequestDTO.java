package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Payload to create or update a dish or menu item in a restaurant's catalog")
public record MenuItemRequestDTO(
    @NotBlank(message = "Item name is mandatory")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Dish or item name", example = "Hambúrguer Smash Rush")
    String name,

    @Schema(description = "Detailed description and ingredients", example = "Pão brioche, 2x smash burger 90g, queijo cheddar inglês e maionese defumada")
    String description,

    @NotNull(message = "Base price is mandatory")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base price cannot be negative")
    @Schema(description = "Base price when no specific size is selected", example = "29.90")
    BigDecimal basePrice,

    @Schema(description = "Whether this dish is currently active and visible on the menu", defaultValue = "true")
    Boolean available,

    @Valid
    @Schema(description = "Optional list of size variations for this item")
    List<MenuItemSizeRequestDTO> sizes,

    @Valid
    @Schema(description = "Optional list of customization/complement groups")
    List<ComplementGroupRequestDTO> complementGroups
) {}

