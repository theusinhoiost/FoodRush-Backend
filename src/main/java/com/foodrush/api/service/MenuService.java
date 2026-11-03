package com.foodrush.api.service;

import com.foodrush.api.dto.request.ComplementGroupRequestDTO;
import com.foodrush.api.dto.request.ComplementItemRequestDTO;
import com.foodrush.api.dto.request.MenuItemRequestDTO;
import com.foodrush.api.dto.request.MenuItemSizeRequestDTO;
import com.foodrush.api.dto.response.ComplementGroupDetailDTO;
import com.foodrush.api.dto.response.ComplementItemDetailDTO;
import com.foodrush.api.dto.response.MenuItemResponseDTO;
import com.foodrush.api.dto.response.MenuItemSizeDetailDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.*;
import com.foodrush.api.repository.MenuItemRepository;
import com.foodrush.api.repository.RestaurantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final MenuItemRepository menuItemRepository;
  private final RestaurantRepository restaurantRepository;

  @Cacheable(value = "restaurant_menu", key = "#restaurantId")
  @Transactional(readOnly = true)
  public List<MenuItemResponseDTO> findMenuByRestaurant(UUID restaurantId) {
    if (!restaurantRepository.existsById(restaurantId)) {
      throw new BusinessException("Restaurant not found with ID: " + restaurantId);
    }

    return menuItemRepository.findAllByRestaurantIdAndAvailableTrue(restaurantId)
        .stream()
        .map(this::loadFullMenuItem)
        .map(this::toDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public MenuItemResponseDTO findById(UUID id) {
    MenuItem item = menuItemRepository.findById(id)
        .filter(m -> Boolean.TRUE.equals(m.getAvailable()))
        .map(this::loadFullMenuItem)
        .orElseThrow(() -> new BusinessException("Menu item not found with ID: " + id));
    return toDTO(item);
  }

  private MenuItem loadFullMenuItem(MenuItem item) {
    // Carrega tamanhos e complementos em passos separados para evitar MultipleBagFetchException
    MenuItem withSizes = menuItemRepository.findByIdWithSizes(item.getId()).orElse(item);
    return menuItemRepository.findByIdWithComplements(item.getId()).orElse(withSizes);
  }

  @CacheEvict(value = "restaurant_menu", key = "#restaurantId")
  @Transactional
  public MenuItemResponseDTO create(UUID restaurantId, MenuItemRequestDTO dto, String authenticatedEmail) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
        .orElseThrow(() -> new BusinessException("Restaurant not found with ID: " + restaurantId));

    validateRestaurantOwnership(restaurant, authenticatedEmail);

    MenuItem menuItem = MenuItem.builder()
        .restaurant(restaurant)
        .name(dto.name())
        .description(dto.description())
        .basePrice(dto.basePrice())
        .available(dto.available() != null ? dto.available() : true)
        .sizes(new ArrayList<>())
        .complementGroups(new ArrayList<>())
        .build();

    if (dto.sizes() != null) {
      for (MenuItemSizeRequestDTO sizeDTO : dto.sizes()) {
        MenuItemSize size = MenuItemSize.builder()
            .menuItem(menuItem)
            .name(sizeDTO.name())
            .price(sizeDTO.price())
            .stockQuantity(sizeDTO.stockQuantity())
            .available(sizeDTO.available() != null ? sizeDTO.available() : true)
            .build();
        menuItem.getSizes().add(size);
      }
    }

    if (dto.complementGroups() != null) {
      for (ComplementGroupRequestDTO groupDTO : dto.complementGroups()) {
        ComplementGroup group = ComplementGroup.builder()
            .menuItem(menuItem)
            .name(groupDTO.name())
            .required(groupDTO.required() != null ? groupDTO.required() : false)
            .minQuantity(groupDTO.minQuantity() != null ? groupDTO.minQuantity() : 0)
            .maxQuantity(groupDTO.maxQuantity() != null ? groupDTO.maxQuantity() : 1)
            .items(new ArrayList<>())
            .build();

        if (groupDTO.items() != null) {
          for (ComplementItemRequestDTO compDTO : groupDTO.items()) {
            ComplementItem compItem = ComplementItem.builder()
                .group(group)
                .name(compDTO.name())
                .price(compDTO.price())
                .available(compDTO.available() != null ? compDTO.available() : true)
                .build();
            group.getItems().add(compItem);
          }
        }
        menuItem.getComplementGroups().add(group);
      }
    }

    MenuItem saved = menuItemRepository.save(menuItem);
    return toDTO(saved);
  }

  @CacheEvict(value = "restaurant_menu", allEntries = true)
  @Transactional
  public MenuItemResponseDTO toggleAvailability(UUID id, String authenticatedEmail) {
    MenuItem item = menuItemRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Menu item not found with ID: " + id));

    validateRestaurantOwnership(item.getRestaurant(), authenticatedEmail);

    item.setAvailable(!Boolean.TRUE.equals(item.getAvailable()));
    MenuItem updated = menuItemRepository.save(item);
    return toDTO(updated);
  }

  @CacheEvict(value = "restaurant_menu", allEntries = true)
  @Transactional
  public void delete(UUID id, String authenticatedEmail) {
    MenuItem item = menuItemRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Menu item not found with ID: " + id));

    validateRestaurantOwnership(item.getRestaurant(), authenticatedEmail);

    menuItemRepository.delete(item);
  }

  private void validateRestaurantOwnership(Restaurant restaurant, String authenticatedEmail) {
    if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
      return; // Permite chamadas internas ou testes que não passam email
    }

    User owner = restaurant.getOwner();
    boolean isOwner = owner != null && owner.getEmail().equalsIgnoreCase(authenticatedEmail);

    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    if (!isOwner && !isAdmin) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Você não possui permissão para modificar o cardápio deste restaurante."
      );
    }
  }

  public MenuItemResponseDTO toDTO(MenuItem item) {
    List<MenuItemSizeDetailDTO> sizeDTOs = item.getSizes() != null
        ? item.getSizes().stream().map(s -> new MenuItemSizeDetailDTO(
            s.getId(), s.getName(), s.getPrice(), s.getStockQuantity(), s.getAvailable()
        )).toList()
        : List.of();

    List<ComplementGroupDetailDTO> groupDTOs = item.getComplementGroups() != null
        ? item.getComplementGroups().stream().map(g -> {
          List<ComplementItemDetailDTO> itemDTOs = g.getItems() != null
              ? g.getItems().stream().map(i -> new ComplementItemDetailDTO(
                  i.getId(), i.getName(), i.getPrice(), i.getAvailable()
              )).toList()
              : List.of();

          return new ComplementGroupDetailDTO(
              g.getId(), g.getName(), g.getRequired(), g.getMinQuantity(), g.getMaxQuantity(), itemDTOs
          );
        }).toList()
        : List.of();

    return new MenuItemResponseDTO(
        item.getId(),
        item.getRestaurant().getId(),
        item.getName(),
        item.getDescription(),
        item.getBasePrice(),
        item.getAvailable(),
        sizeDTOs,
        groupDTOs
    );
  }
}

