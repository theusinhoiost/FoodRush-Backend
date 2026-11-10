package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.UserPatchRequestDTO;
import com.foodrush.api.dto.request.UserRequestDTO;
import com.foodrush.api.dto.response.UserResponseDTO;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private GeocodingService geocodingService;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("Deve cadastrar um usuário com sucesso quando os dados forem válidos")
  void shouldCreateUserSuccessfully() {
    UserRequestDTO dto = new UserRequestDTO(
      "João Silva",
      "joao@email.com",
      "senha123",
      "11999998888",
      "12345678909",
      "CLIENT",
      null
    );

    User savedUser = new User();
    savedUser.setId(UUID.randomUUID());
    savedUser.setName(dto.name());
    savedUser.setEmail(dto.email());
    savedUser.setPassword("hashed_password");
    savedUser.setPhone(dto.phone());
    savedUser.setCpf(dto.cpf());
    savedUser.setRole(RoleType.CLIENT);
    savedUser.setActive(true);

    when(userRepository.existsByPhone(dto.phone())).thenReturn(false);
    when(userRepository.existsByEmail(dto.email())).thenReturn(false);
    when(userRepository.existsByCpf(dto.cpf())).thenReturn(false);
    when(passwordEncoder.encode(dto.password())).thenReturn("hashed_password");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    UserResponseDTO response = userService.create(dto);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("João Silva");
    assertThat(response.email()).isEqualTo("joao@email.com");
    assertThat(response.role()).isEqualTo("CLIENT");
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar criar usuário com e-mail já cadastrado")
  void shouldThrowExceptionWhenEmailAlreadyExistsOnCreate() {
    UserRequestDTO dto = new UserRequestDTO(
      "João Silva",
      "joao@email.com",
      "senha123",
      "11999998888",
      "12345678909",
      "CLIENT",
      null
    );

    when(userRepository.existsByPhone(dto.phone())).thenReturn(false);
    when(userRepository.existsByEmail(dto.email())).thenReturn(true);

    assertThatThrownBy(() -> userService.create(dto))
      .isInstanceOf(BusinessException.class)
      .hasMessage("Já existe um usuário cadastrado com este e-mail.");

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve bloquear tentativa de criar conta com perfil ADMIN no cadastro")
  void shouldThrowExceptionWhenTryingToRegisterAsAdmin() {
    UserRequestDTO dto = new UserRequestDTO(
      "Hacker Silva",
      "hacker@email.com",
      "senha123",
      "11999998888",
      "12345678909",
      "ADMIN",
      null
    );

    when(userRepository.existsByPhone(dto.phone())).thenReturn(false);
    when(userRepository.existsByEmail(dto.email())).thenReturn(false);
    when(userRepository.existsByCpf(dto.cpf())).thenReturn(false);

    assertThatThrownBy(() -> userService.create(dto))
      .isInstanceOf(BusinessException.class)
      .hasMessage("Não é permitido registrar contas com perfil ADMIN publicamente.");

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve atualizar parcialmente um usuário (patch) com sucesso")
  void shouldPatchUserSuccessfully() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User();
    existingUser.setId(userId);
    existingUser.setName("João Antigo");
    existingUser.setEmail("antigo@email.com");
    existingUser.setPhone("11988887777");
    existingUser.setRole(RoleType.CLIENT);
    existingUser.setActive(true);

    UserPatchRequestDTO patchDto = new UserPatchRequestDTO(
      "João Novo",
      "novo@email.com",
      "11999998888",
      null
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
    when(userRepository.existsByPhone("11999998888")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponseDTO response = userService.patch(userId, patchDto);

    assertThat(response.name()).isEqualTo("João Novo");
    assertThat(response.email()).isEqualTo("novo@email.com");
    assertThat(response.phone()).isEqualTo("11999998888");
    verify(userRepository, times(1)).save(existingUser);
  }
}
