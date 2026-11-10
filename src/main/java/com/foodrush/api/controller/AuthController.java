package com.foodrush.api.controller;

import com.foodrush.api.dto.request.LoginRequestDTO;
import com.foodrush.api.dto.request.RefreshTokenRequestDTO;
import com.foodrush.api.dto.request.UserRequestDTO;
import com.foodrush.api.dto.response.AuthResponseDTO;
import com.foodrush.api.dto.response.UserResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.security.JwtTokenProvider;
import com.foodrush.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(
  name = "Authentication",
  description = "Public endpoints for user registration, credential authentication, and JWT token renewal"
)
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final UserService userService;

  public AuthController(
    AuthenticationManager authenticationManager,
    JwtTokenProvider tokenProvider,
    UserService userService
  ) {
    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
    this.userService = userService;
  }

  @PostMapping("/register")
  @Operation(
    summary = "Register a new user account",
    description = "Creates a new user account with BCrypt-hashed password, validates CPF/phone/email uniqueness, and optionally resolves the user's address and coordinates via CEP."
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "201",
        description = "User registered successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = UserResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload or field validation error",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
      @ApiResponse(
        responseCode = "422",
        description = "Business rule violation (e.g., email, phone, or CPF already registered)",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<UserResponseDTO> register(
    @RequestBody @Valid UserRequestDTO dto
  ) {
    UserResponseDTO createdUser = userService.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PostMapping("/login")
  @Operation(
    summary = "Authenticate user and issue JWT tokens",
    description = "Validates user credentials (email and password) and returns a signed HS512 JWT `accessToken` and `refreshToken`."
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Authenticated successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = AuthResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Missing or invalid login fields",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "Invalid email or password",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<AuthResponseDTO> login(
    @RequestBody @Valid LoginRequestDTO loginDTO
  ) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        loginDTO.email(),
        loginDTO.password()
      )
    );

    String accessToken = tokenProvider.generateAccessToken(authentication);
    String refreshToken = tokenProvider.generateRefreshToken(authentication);

    return ResponseEntity.ok(new AuthResponseDTO(accessToken, refreshToken));
  }

  @PostMapping("/refresh")
  @Operation(
    summary = "Refresh JWT access token",
    description = "Validates a non-expired `refreshToken` and issues a new short-lived `accessToken`."
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "New access token issued successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = AuthResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "422",
        description = "Invalid or expired refresh token",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<AuthResponseDTO> refreshToken(
    @RequestBody @Valid RefreshTokenRequestDTO requestDTO
  ) {
    if (
      !tokenProvider.validateToken(requestDTO.refreshToken()) ||
      tokenProvider.isAccessToken(requestDTO.refreshToken())
    ) {
      throw new BusinessException("Refresh token inválido ou expirado.");
    }

    String username = tokenProvider.getUsernameFromJWT(
      requestDTO.refreshToken()
    );
    String newAccessToken = tokenProvider.generateAccessTokenFromUsername(
      username
    );

    return ResponseEntity.ok(
      new AuthResponseDTO(newAccessToken, requestDTO.refreshToken())
    );
  }
}