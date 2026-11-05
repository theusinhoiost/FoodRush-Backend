package com.foodrush.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Payload to define a group of complements or customization options for a dish")
public record ComplementGroupRequestDTO(
    @NotBlank(message = "Group name is mandatory")
    @Size(max = 80, message = "Name must not exceed 80 characters")
    @Schema(description = "Name of the group", example = "Escolha seu Molho")
    String name,

    @Schema(description = "Whether picking an option from this group is mandatory", defaultValue = "false")
    Boolean required,

    @Schema(description = "Minimum number of items the customer must select", defaultValue = "0", example = "0")
    Integer minQuantity,

    @NotNull(message = "maxQuantity is mandatory")
    @Schema(description = "Maximum number of items the customer can select", defaultValue = "1", example = "2")
    Integer maxQuantity,

    @Valid
    @Schema(description = "List of complement items in this group")
    List<ComplementItemRequestDTO> items
) {}

