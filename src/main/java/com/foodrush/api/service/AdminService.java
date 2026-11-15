package com.foodrush.api.service;

import com.foodrush.api.dto.request.AdminUpdateCommissionRequestDTO;
import com.foodrush.api.dto.response.AdminMetricsResponseDTO;
import com.foodrush.api.dto.response.AdminRestaurantResponseDTO;
import com.foodrush.api.dto.response.AdminUserResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Order;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.OrderRepository;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  private final RestaurantRepository restaurantRepository;
  private final UserRepository userRepository;
  private final OrderRepository orderRepository;

  @Transactional(readOnly = true)
  public List<AdminRestaurantResponseDTO> getAllRestaurants() {
    return restaurantRepository.findAll().stream()
        .map(this::toRestaurantDTO)
        .toList();
  }

  @Transactional
  public AdminRestaurantResponseDTO updateCommissionRate(UUID restaurantId, AdminUpdateCommissionRequestDTO request) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
        .orElseThrow(() -> new BusinessException("Restaurant not found with ID: " + restaurantId));

    restaurant.setCommissionRate(request.commissionRate());
    Restaurant saved = restaurantRepository.save(restaurant);
    log.info("Admin updated commission rate for restaurant {} ({}) to {}%",
        saved.getId(), saved.getTradeName(), saved.getCommissionRate());

    return toRestaurantDTO(saved);
  }

  @Transactional(readOnly = true)
  public List<AdminUserResponseDTO> getAllUsers() {
    return userRepository.findAll().stream()
        .map(u -> new AdminUserResponseDTO(
            u.getId(),
            u.getName(),
            u.getEmail(),
            u.getPhone(),
            u.getCpf(),
            u.getRole(),
            Boolean.TRUE.equals(u.getActive())
        ))
        .toList();
  }

  @Transactional
  public AdminUserResponseDTO toggleUserStatus(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException("User not found with ID: " + userId));

    boolean newStatus = !Boolean.TRUE.equals(user.getActive());
    user.setActive(newStatus);
    User saved = userRepository.save(user);
    log.info("Admin toggled active status for user {} ({}) to {}", saved.getId(), saved.getEmail(), newStatus);

    return new AdminUserResponseDTO(
        saved.getId(),
        saved.getName(),
        saved.getEmail(),
        saved.getPhone(),
        saved.getCpf(),
        saved.getRole(),
        Boolean.TRUE.equals(saved.getActive())
    );
  }

  @Transactional(readOnly = true)
  public AdminMetricsResponseDTO getMetrics() {
    long totalRestaurants = restaurantRepository.count();
    long connectedRestaurants = restaurantRepository.countByMpConnectedTrue();
    long totalUsers = userRepository.count();
    long totalOrders = orderRepository.count();

    BigDecimal totalGross = orderRepository.sumGrossVolume();
    if (totalGross == null) {
      totalGross = BigDecimal.ZERO;
    }

    // Calculate platform fees sum across paid orders
    List<Order> nonCanceledOrders = orderRepository.findAll().stream()
        .filter(o -> o.getStatus() != OrderStatus.CANCELED && o.getStatus() != OrderStatus.CREATED)
        .toList();

    BigDecimal totalFees = BigDecimal.ZERO;
    for (Order o : nonCanceledOrders) {
      BigDecimal rate = (o.getRestaurant() != null && o.getRestaurant().getCommissionRate() != null)
          ? o.getRestaurant().getCommissionRate()
          : new BigDecimal("10.00");
      BigDecimal fee = o.getTotal().multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      totalFees = totalFees.add(fee);
    }

    return new AdminMetricsResponseDTO(
        totalRestaurants,
        connectedRestaurants,
        totalUsers,
        totalOrders,
        totalGross,
        totalFees
    );
  }

  private AdminRestaurantResponseDTO toRestaurantDTO(Restaurant r) {
    String ownerName = r.getOwner() != null ? r.getOwner().getName() : "N/A";
    String ownerEmail = r.getOwner() != null ? r.getOwner().getEmail() : "N/A";
    UUID ownerId = r.getOwner() != null ? r.getOwner().getId() : null;

    return new AdminRestaurantResponseDTO(
        r.getId(),
        r.getTradeName(),
        r.getCnpj(),
        r.getPhone(),
        ownerId,
        ownerName,
        ownerEmail,
        r.getCommissionRate() != null ? r.getCommissionRate() : new BigDecimal("10.00"),
        Boolean.TRUE.equals(r.getMpConnected()),
        r.getMpCollectorId(),
        r.getMpConnectedAt(),
        Boolean.TRUE.equals(r.getIsOpen()),
        r.getBaseDeliveryFee()
    );
  }
}
