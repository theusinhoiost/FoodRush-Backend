package com.foodrush.api.service;

import com.foodrush.api.dto.request.UserPatchRequestDTO;
import com.foodrush.api.dto.request.UserRequestDTO;
import com.foodrush.api.dto.response.UserResponseDTO;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Address;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final GeocodingService geocodingService;

  public UserService(
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    GeocodingService geocodingService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.geocodingService = geocodingService;
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserResponseDTO create(UserRequestDTO dto) {
    if (userRepository.existsByPhone(dto.phone())) {
      throw new BusinessException("Já existe um usuário cadastrado com este telefone.");
    }
    if (userRepository.existsByEmail(dto.email())) {
      throw new BusinessException("Já existe um usuário cadastrado com este e-mail.");
    }
    if (dto.cpf() != null && userRepository.existsByCpf(dto.cpf())) {
      throw new BusinessException("Já existe um usuário cadastrado com este CPF.");
    }

    RoleType role = parseRole(dto.role());
    if (role == RoleType.ADMIN) {
      throw new BusinessException(
        "Não é permitido registrar contas com perfil ADMIN publicamente."
      );
    }
    String encodedPassword = passwordEncoder != null
      ? passwordEncoder.encode(dto.password())
      : dto.password();
    Address resolvedAddress = dto.address() != null
      ? geocodingService.resolveAddress(dto.address())
      : null;

    User user = User.builder()
      .name(dto.name())
      .email(dto.email())
      .password(encodedPassword)
      .phone(dto.phone())
      .cpf(dto.cpf())
      .role(role)
      .active(true)
      .address(resolvedAddress)
      .build();

    User savedUser = userRepository.save(user);
    return UserResponseDTO.fromEntity(savedUser);
  }

  @Cacheable(value = "users")
  @Transactional(readOnly = true)
  public List<UserResponseDTO> findAll() {
    return userRepository
      .findAll()
      .stream()
      .map(UserResponseDTO::fromEntity)
      .collect(Collectors.toList());
  }

  @Cacheable(value = "users", key = "#id")
  @Transactional(readOnly = true)
  public UserResponseDTO findById(UUID id) {
    User user = userRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    return UserResponseDTO.fromEntity(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserResponseDTO toggleActiveStatus(UUID id) {
    User user = userRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

    user.setActive(!user.getActive());
    User updatedUser = userRepository.save(user);
    return UserResponseDTO.fromEntity(updatedUser);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserResponseDTO update(UUID id, UserRequestDTO dto) {
    User user = userRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

    if (
      !user.getEmail().equals(dto.email()) &&
      userRepository.existsByEmail(dto.email())
    ) {
      throw new BusinessException(
        "Já existe outro usuário cadastrado com este e-mail."
      );
    }

    if (
      !user.getPhone().equals(dto.phone()) &&
      userRepository.existsByPhone(dto.phone())
    ) {
      throw new BusinessException(
        "Já existe outro usuário cadastrado com este telefone."
      );
    }

    if (
      dto.cpf() != null &&
      !dto.cpf().equals(user.getCpf()) &&
      userRepository.existsByCpf(dto.cpf())
    ) {
      throw new BusinessException(
        "Já existe outro usuário cadastrado com este CPF."
      );
    }

    RoleType role = parseRole(dto.role());
    if (role == RoleType.ADMIN) {
      throw new BusinessException(
        "Não é permitido registrar contas com perfil ADMIN publicamente."
      );
    }
    String encodedPassword = passwordEncoder != null
      ? passwordEncoder.encode(dto.password())
      : dto.password();

    user.setName(dto.name());
    user.setEmail(dto.email());
    user.setPassword(encodedPassword);
    user.setPhone(dto.phone());
    user.setCpf(dto.cpf());
    user.setRole(role);

    if (dto.address() != null) {
      user.setAddress(geocodingService.resolveAddress(dto.address()));
    }

    User updatedUser = userRepository.save(user);
    return UserResponseDTO.fromEntity(updatedUser);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserResponseDTO patch(UUID id, UserPatchRequestDTO dto) {
    User user = userRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

    if (dto.name() != null && !dto.name().isBlank()) {
      user.setName(dto.name());
    }

    if (dto.email() != null && !dto.email().isBlank()) {
      if (
        !user.getEmail().equals(dto.email()) &&
        userRepository.existsByEmail(dto.email())
      ) {
        throw new BusinessException(
          "Já existe outro usuário cadastrado com este e-mail."
        );
      }
      user.setEmail(dto.email());
    }

    if (dto.phone() != null && !dto.phone().isBlank()) {
      if (
        !user.getPhone().equals(dto.phone()) &&
        userRepository.existsByPhone(dto.phone())
      ) {
        throw new BusinessException(
          "Já existe outro usuário cadastrado com este telefone."
        );
      }
      user.setPhone(dto.phone());
    }

    if (dto.address() != null) {
      user.setAddress(geocodingService.resolveAddress(dto.address()));
    }

    User updatedUser = userRepository.save(user);
    return UserResponseDTO.fromEntity(updatedUser);
  }

  private RoleType parseRole(String roleStr) {
    if (roleStr == null || roleStr.isBlank()) {
      return RoleType.CLIENT;
    }
    try {
      return RoleType.valueOf(roleStr.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException("Perfil (role) inválido: " + roleStr);
    }
  }
}
