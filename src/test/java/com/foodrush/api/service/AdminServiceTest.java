package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.AdminUpdateCommissionRequestDTO;
import com.foodrush.api.dto.response.AdminMetricsResponseDTO;
import com.foodrush.api.dto.response.AdminRestaurantResponseDTO;
import com.foodrush.api.dto.response.AdminUserResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Order;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.OrderRepository;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.repository.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock
  private RestaurantRepository restaurantRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private AdminService adminService;

  @Test
  @DisplayName("Should list all restaurants with commission and MP status")
  void shouldGetAllRestaurants() {
    User owner = User.builder()
        .id(UUID.randomUUID())
        .name("Carlos Dono")
        .email("carlos@rest.com")
        .build();

    Restaurant r = Restaurant.builder()
        .id(UUID.randomUUID())
        .owner(owner)
        .tradeName("Burger King")
        .cnpj("12345678000199")
        .phone("11999998888")
        .commissionRate(new BigDecimal("10.00"))
        .mpConnected(true)
        .mpCollectorId("COL-123")
        .isOpen(true)
        .baseDeliveryFee(new BigDecimal("5.00"))
        .build();

    when(restaurantRepository.findAll()).thenReturn(List.of(r));

    List<AdminRestaurantResponseDTO> result = adminService.getAllRestaurants();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tradeName()).isEqualTo("Burger King");
    assertThat(result.getFirst().commissionRate()).isEqualByComparingTo("10.00");
    assertThat(result.getFirst().mpConnected()).isTrue();
  }

  @Test
  @DisplayName("Should update restaurant commission rate")
  void shouldUpdateCommissionRate() {
    UUID restId = UUID.randomUUID();
    Restaurant r = Restaurant.builder()
        .id(restId)
        .tradeName("Pizza Express")
        .commissionRate(new BigDecimal("10.00"))
        .build();

    when(restaurantRepository.findById(restId)).thenReturn(Optional.of(r));
    when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

    AdminUpdateCommissionRequestDTO req = new AdminUpdateCommissionRequestDTO(new BigDecimal("8.50"));
    AdminRestaurantResponseDTO updated = adminService.updateCommissionRate(restId, req);

    assertThat(updated.commissionRate()).isEqualByComparingTo("8.50");
    verify(restaurantRepository).save(r);
  }

  @Test
  @DisplayName("Should throw exception when updating commission of nonexistent restaurant")
  void shouldThrowExceptionWhenRestaurantNotFound() {
    UUID restId = UUID.randomUUID();
    when(restaurantRepository.findById(restId)).thenReturn(Optional.empty());

    AdminUpdateCommissionRequestDTO req = new AdminUpdateCommissionRequestDTO(new BigDecimal("8.50"));

    assertThatThrownBy(() -> adminService.updateCommissionRate(restId, req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Restaurant not found");
  }

  @Test
  @DisplayName("Should list all users and toggle active status")
  void shouldListUsersAndToggleStatus() {
    UUID userId = UUID.randomUUID();
    User u = User.builder()
        .id(userId)
        .name("João Cliente")
        .email("joao@email.com")
        .role(RoleType.CLIENT)
        .active(true)
        .build();

    when(userRepository.findAll()).thenReturn(List.of(u));
    when(userRepository.findById(userId)).thenReturn(Optional.of(u));
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    List<AdminUserResponseDTO> users = adminService.getAllUsers();
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().active()).isTrue();

    AdminUserResponseDTO toggled = adminService.toggleUserStatus(userId);
    assertThat(toggled.active()).isFalse();
  }

  @Test
  @DisplayName("Should calculate metrics including total gross volume and platform split fees")
  void shouldCalculateMetrics() {
    when(restaurantRepository.count()).thenReturn(5L);
    when(restaurantRepository.countByMpConnectedTrue()).thenReturn(3L);
    when(userRepository.count()).thenReturn(20L);
    when(orderRepository.count()).thenReturn(10L);
    when(orderRepository.sumGrossVolume()).thenReturn(new BigDecimal("1000.00"));

    Restaurant r = Restaurant.builder()
        .commissionRate(new BigDecimal("10.00"))
        .build();

    Order o1 = Order.builder()
        .status(OrderStatus.PAID)
        .restaurant(r)
        .total(new BigDecimal("100.00"))
        .build();

    Order o2 = Order.builder()
        .status(OrderStatus.DELIVERED)
        .restaurant(r)
        .total(new BigDecimal("200.00"))
        .build();

    when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

    AdminMetricsResponseDTO metrics = adminService.getMetrics();

    assertThat(metrics.totalRestaurants()).isEqualTo(5);
    assertThat(metrics.connectedRestaurants()).isEqualTo(3);
    assertThat(metrics.totalUsers()).isEqualTo(20);
    assertThat(metrics.totalOrders()).isEqualTo(10);
    assertThat(metrics.totalGrossVolume()).isEqualByComparingTo("1000.00");
    // 10% of 100 = 10, 10% of 200 = 20 -> 30.00
    assertThat(metrics.totalPlatformFees()).isEqualByComparingTo("30.00");
  }
}
