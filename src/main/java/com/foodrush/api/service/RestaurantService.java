package com.foodrush.api.service;

import com.foodrush.api.dto.request.RestaurantPatchRequestDTO;
import com.foodrush.api.dto.request.RestaurantRequestDTO;
import com.foodrush.api.dto.response.RestaurantResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Address;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final UserRepository userRepository;
  private final GeocodingService geocodingService;

  public RestaurantService(
    RestaurantRepository restaurantRepository,
    UserRepository userRepository,
    GeocodingService geocodingService
  ) {
    this.restaurantRepository = restaurantRepository;
    this.userRepository = userRepository;
    this.geocodingService = geocodingService;
  }

  public RestaurantResponseDTO create(RestaurantRequestDTO dto) {
    return create(dto, null);
  }

  @CacheEvict(value = "restaurants", allEntries = true)
  @Transactional
  public RestaurantResponseDTO create(RestaurantRequestDTO dto, String authenticatedEmail) {
    if (restaurantRepository.existsByCnpj(dto.cnpj())) {
      throw new BusinessException(
        "Já existe um restaurante cadastrado com este CNPJ."
      );
    }
    if (restaurantRepository.existsByPhone(dto.phone())) {
      throw new BusinessException(
        "Já existe um restaurante cadastrado com este telefone."
      );
    }
    User owner = userRepository
      .findById(dto.ownerId())
      .orElseThrow(() ->
        new BusinessException(
          "Proprietário (User) não encontrado com o ID informado."
        )
      );

    if (authenticatedEmail != null && !authenticatedEmail.isBlank() && !isAdmin()) {
      if (!owner.getEmail().equalsIgnoreCase(authenticatedEmail)) {
        throw new AccessDeniedException(
          "Você só pode cadastrar um restaurante para o seu próprio usuário."
        );
      }
    }

    Address resolvedAddress = geocodingService.resolveAddress(dto.address());

    Restaurant restaurant = Restaurant.builder()
      .owner(owner)
      .tradeName(dto.tradeName())
      .cnpj(dto.cnpj())
      .phone(dto.phone())
      .baseDeliveryFee(dto.baseDeliveryFee())
      .latitude(resolvedAddress.getLatitude())
      .longitude(resolvedAddress.getLongitude())
      .address(resolvedAddress)
      .isOpen(false)
      .build();

    Restaurant savedRestaurant = restaurantRepository.save(restaurant);
    return RestaurantResponseDTO.fromEntity(savedRestaurant);
  }

  @Cacheable(value = "restaurants")
  @Transactional(readOnly = true)
  public List<RestaurantResponseDTO> findAll() {
    return restaurantRepository
      .findAll()
      .stream()
      .map(RestaurantResponseDTO::fromEntity)
      .collect(Collectors.toList());
  }

  @Cacheable(value = "restaurants", key = "#id")
  @Transactional(readOnly = true)
  public RestaurantResponseDTO findById(UUID id) {
    Restaurant restaurant = restaurantRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Restaurante não encontrado."));
    return RestaurantResponseDTO.fromEntity(restaurant);
  }

  public RestaurantResponseDTO toggleOpenStatus(UUID id) {
    return toggleOpenStatus(id, null);
  }

  @CacheEvict(value = "restaurants", allEntries = true)
  @Transactional
  public RestaurantResponseDTO toggleOpenStatus(UUID id, String authenticatedEmail) {
    Restaurant restaurant = restaurantRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Restaurante não encontrado."));

    validateRestaurantOwnership(restaurant, authenticatedEmail);

    restaurant.setIsOpen(!restaurant.getIsOpen());
    Restaurant updated = restaurantRepository.save(restaurant);
    return RestaurantResponseDTO.fromEntity(updated);
  }

  public RestaurantResponseDTO update(UUID id, RestaurantRequestDTO dto) {
    return update(id, dto, null);
  }

  @CacheEvict(value = "restaurants", allEntries = true)
  @Transactional
  public RestaurantResponseDTO update(UUID id, RestaurantRequestDTO dto, String authenticatedEmail) {
    Restaurant restaurant = restaurantRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Restaurante não encontrado."));

    validateRestaurantOwnership(restaurant, authenticatedEmail);

    if (authenticatedEmail != null && !authenticatedEmail.isBlank() && !isAdmin()) {
      if (restaurant.getOwner() != null && !restaurant.getOwner().getId().equals(dto.ownerId())) {
        throw new AccessDeniedException(
          "Apenas administradores podem transferir a propriedade do restaurante."
        );
      }
    }

    if (
      !restaurant.getCnpj().equals(dto.cnpj()) &&
      restaurantRepository.existsByCnpj(dto.cnpj())
    ) {
      throw new BusinessException(
        "Já existe outro restaurante cadastrado com este CNPJ."
      );
    }

    User owner = userRepository
      .findById(dto.ownerId())
      .orElseThrow(() ->
        new BusinessException(
          "Proprietário (User) não encontrado com o ID informado."
        )
      );

    Address resolvedAddress = geocodingService.resolveAddress(dto.address());

    restaurant.setOwner(owner);
    restaurant.setTradeName(dto.tradeName());
    restaurant.setCnpj(dto.cnpj());
    restaurant.setPhone(dto.phone());
    restaurant.setBaseDeliveryFee(dto.baseDeliveryFee());
    restaurant.setLatitude(resolvedAddress.getLatitude());
    restaurant.setLongitude(resolvedAddress.getLongitude());
    restaurant.setAddress(resolvedAddress);

    Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
    return RestaurantResponseDTO.fromEntity(updatedRestaurant);
  }

  public RestaurantResponseDTO patch(UUID id, RestaurantPatchRequestDTO dto) {
    return patch(id, dto, null);
  }

  @CacheEvict(value = "restaurants", allEntries = true)
  @Transactional
  public RestaurantResponseDTO patch(UUID id, RestaurantPatchRequestDTO dto, String authenticatedEmail) {
    Restaurant restaurant = restaurantRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Restaurante não encontrado."));

    validateRestaurantOwnership(restaurant, authenticatedEmail);

    if (dto.ownerId() != null && authenticatedEmail != null && !authenticatedEmail.isBlank() && !isAdmin()) {
      if (restaurant.getOwner() != null && !restaurant.getOwner().getId().equals(dto.ownerId())) {
        throw new AccessDeniedException(
          "Apenas administradores podem transferir a propriedade do restaurante."
        );
      }
    }

    if (dto.tradeName() != null) {
      restaurant.setTradeName(dto.tradeName());
    }

    if (dto.cnpj() != null) {
      if (
        !restaurant.getCnpj().equals(dto.cnpj()) &&
        restaurantRepository.existsByCnpj(dto.cnpj())
      ) {
        throw new BusinessException(
          "Já existe outro restaurante cadastrado com este CNPJ."
        );
      }
      restaurant.setCnpj(dto.cnpj());
    }

    if (dto.phone() != null) {
      if (
        !restaurant.getPhone().equals(dto.phone()) &&
        restaurantRepository.existsByPhone(dto.phone())
      ) {
        throw new BusinessException(
          "Já existe outro restaurante cadastrado com este telefone."
        );
      }
      restaurant.setPhone(dto.phone());
    }

    if (dto.ownerId() != null) {
      User owner = userRepository
        .findById(dto.ownerId())
        .orElseThrow(() ->
          new BusinessException(
            "Proprietário (User) não encontrado com o ID informado."
          )
        );
      restaurant.setOwner(owner);
    }

    if (dto.baseDeliveryFee() != null) {
      restaurant.setBaseDeliveryFee(dto.baseDeliveryFee());
    }

    if (dto.address() != null) {
      Address resolvedAddress = geocodingService.resolveAddress(dto.address());
      restaurant.setLatitude(resolvedAddress.getLatitude());
      restaurant.setLongitude(resolvedAddress.getLongitude());
      restaurant.setAddress(resolvedAddress);
    }

    if (dto.isOpen() != null) {
      restaurant.setIsOpen(dto.isOpen());
    }

    Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
    return RestaurantResponseDTO.fromEntity(updatedRestaurant);
  }

  private void validateRestaurantOwnership(Restaurant restaurant, String authenticatedEmail) {
    if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
      return;
    }

    User owner = restaurant.getOwner();
    boolean isOwner = owner != null && owner.getEmail().equalsIgnoreCase(authenticatedEmail);
    boolean isAdmin = isAdmin();

    if (!isOwner && !isAdmin) {
      throw new AccessDeniedException(
        "Você não possui permissão para modificar as informações deste restaurante."
      );
    }
  }

  private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }
}
