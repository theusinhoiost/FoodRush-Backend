package com.foodrush.api.controller;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.request.LoginRequestDTO;
import com.foodrush.api.dto.request.RefreshTokenRequestDTO;
import com.foodrush.api.dto.request.UserRequestDTO;
import com.foodrush.api.dto.response.AddressResponseDTO;
import com.foodrush.api.dto.response.AuthResponseDTO;
import com.foodrush.api.dto.response.UserResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.security.JwtTokenProvider;
import com.foodrush.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    @Test
    @DisplayName("Deve registrar um usuário com sucesso quando os dados forem válidos")
    void testRegisterUser_Success() {
        AddressRequestDTO addressRequest = new AddressRequestDTO("01001000", "100", "Apto 10");

        UserRequestDTO userRequestDTO = new UserRequestDTO(
                "Matheus Silva",
                "matheus@foodrush.com",
                "123456",
                "11991112222",
                "52998224725",
                "CLIENT",
                addressRequest
        );

        AddressResponseDTO addressResponse = new AddressResponseDTO(
                UUID.randomUUID(),
                "01001000",
                "Praça da Sé",
                "100",
                "Apto 10",
                "Sé",
                "São Paulo",
                "SP",
                new BigDecimal("-23.550520"),
                new BigDecimal("-46.633308")
        );

        UserResponseDTO userResponseDTO = new UserResponseDTO(
                UUID.randomUUID(),
                "Matheus Silva",
                "matheus@foodrush.com",
                "11991112222",
                "52998224725",
                "CLIENT",
                true,
                addressResponse,
                OffsetDateTime.now()
        );

        when(userService.create(any(UserRequestDTO.class))).thenReturn(userResponseDTO);

        ResponseEntity<UserResponseDTO> response = authController.register(userRequestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userResponseDTO.id(), response.getBody().id());
        assertEquals(userResponseDTO.name(), response.getBody().name());
        assertEquals(userResponseDTO.email(), response.getBody().email());
        assertEquals(userResponseDTO.role(), response.getBody().role());
        assertEquals(userResponseDTO.address(), response.getBody().address());

        verify(userService, times(1)).create(any(UserRequestDTO.class));
    }

    @Test
    @DisplayName("Deve propagar BusinessException quando o cadastro falhar nas regras de negócio do serviço")
    void testRegisterUser_BusinessException() {
        UserRequestDTO userRequestDTO = new UserRequestDTO(
                "Matheus Silva",
                "matheus@foodrush.com",
                "123456",
                "11991112222",
                "52998224725",
                "CLIENT",
                null
        );

        when(userService.create(any(UserRequestDTO.class)))
                .thenThrow(new BusinessException("E-mail já cadastrado no sistema."));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authController.register(userRequestDTO)
        );

        assertEquals("E-mail já cadastrado no sistema.", exception.getMessage());
        verify(userService, times(1)).create(any(UserRequestDTO.class));
    }

    @Test
    @DisplayName("Deve autenticar o usuário com sucesso e retornar os tokens")
    void testLoginUser_Success() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(
                "matheus@foodrush.com",
                "123456"
        );
        String accessToken = "access_token_123";
        String refreshToken = "refresh_token_456";

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(refreshToken);

        ResponseEntity<AuthResponseDTO> response = authController.login(loginRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(accessToken, response.getBody().accessToken());
        assertEquals(refreshToken, response.getBody().refreshToken());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException ao tentar login com credenciais inválidas")
    void testLoginUser_InvalidCredentials() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(
                "matheus@foodrush.com",
                "senhaErrada"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThrows(BadCredentialsException.class, () ->
                authController.login(loginRequestDTO)
        );
    }

    @Test
    @DisplayName("Deve renovar o access token com sucesso quando o refresh token for válido")
    void testRefreshToken_Success() {
        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(
                "valid_refresh_token"
        );
        String newAccessToken = "new_access_token_789";

        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(anyString())).thenReturn("matheus@foodrush.com");
        when(tokenProvider.generateAccessTokenFromUsername(anyString())).thenReturn(newAccessToken);

        ResponseEntity<AuthResponseDTO> response = authController.refreshToken(requestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(newAccessToken, response.getBody().accessToken());
        assertEquals(requestDTO.refreshToken(), response.getBody().refreshToken());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o refresh token for inválido ou expirado")
    void testRefreshToken_InvalidToken() {
        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(
                "invalid_refresh_token"
        );

        when(tokenProvider.validateToken(anyString())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authController.refreshToken(requestDTO)
        );

        assertEquals("Refresh token inválido ou expirado.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar tentativa de usar um Access Token no endpoint de Refresh")
    void testRefreshToken_RejectAccessToken() {
        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(
                "access_token_used_as_refresh"
        );

        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenProvider.isAccessToken(anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authController.refreshToken(requestDTO)
        );

        assertEquals("Refresh token inválido ou expirado.", exception.getMessage());
    }
}
