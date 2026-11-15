package com.foodrush.api.dto.response;

import com.foodrush.api.enums.RoleType;
import java.util.UUID;

public record AdminUserResponseDTO(
    UUID id,
    String name,
    String email,
    String phone,
    String cpf,
    RoleType role,
    boolean active
) {}
