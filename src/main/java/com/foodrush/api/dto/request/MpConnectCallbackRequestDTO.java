package com.foodrush.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MpConnectCallbackRequestDTO(
    @NotNull(message = "Restaurant ID is mandatory")
    UUID restaurantId,
    String code,
    String collectorId
) {}
