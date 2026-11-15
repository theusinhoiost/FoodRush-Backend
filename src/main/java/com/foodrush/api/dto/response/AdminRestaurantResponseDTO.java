package com.foodrush.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRestaurantResponseDTO(
    UUID id,
    String tradeName,
    String cnpj,
    String phone,
    UUID ownerId,
    String ownerName,
    String ownerEmail,
    BigDecimal commissionRate,
    boolean mpConnected,
    String mpCollectorId,
    OffsetDateTime mpConnectedAt,
    boolean isOpen,
    BigDecimal baseDeliveryFee
) {}
