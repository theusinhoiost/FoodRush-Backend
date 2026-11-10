package com.foodrush.api.dto.request;

import com.foodrush.api.validation.ValidCPF;
import com.foodrush.api.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for registering or updating a user")
public record UserRequestDTO(
  @Schema(description = "Full name of the user", example = "Matheus Silva")
  @NotBlank(message = "O nome é obrigatório")
  @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
  String name,

  @Schema(
    description = "Unique email address used for login",
    example = "matheus@foodrush.com"
  )
  @NotBlank(message = "O e-mail é obrigatório")
  @Email(message = "Formato de e-mail inválido")
  @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
  String email,

  @Schema(
    description = "Account password (minimum 6 characters)",
    example = "123456"
  )
  @NotBlank(message = "A senha é obrigatória")
  @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
  String password,

  @Schema(
    description = "Brazilian mobile phone number with area code (DDD)",
    example = "11991112222"
  )
  @NotBlank(message = "O telefone é obrigatório")
  @ValidPhone(message = "O número de celular informado é inválido")
  String phone,

  @Schema(
    description = "Valid 11-digit Brazilian individual taxpayer ID (CPF)",
    example = "52998224725"
  )
  @ValidCPF(message = "O CPF informado é inválido")
  String cpf,

  @Schema(
    description = "Access role assigned to the user (ADMIN cannot be self-registered)",
    example = "CLIENT",
    allowableValues = { "CLIENT", "RESTAURANT", "COURIER" }
  )
  @NotBlank(message = "O perfil (role) é obrigatório")
  @Size(max = 20, message = "O perfil deve ter no máximo 20 caracteres")
  String role,

  @Schema(description = "Optional user delivery/home address")
  @Valid
  AddressRequestDTO address
) {}
