package com.foodrush.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foodrush.api.dto.request.AdminUpdateCommissionRequestDTO;
import com.foodrush.api.dto.response.AdminMetricsResponseDTO;
import com.foodrush.api.dto.response.AdminRestaurantResponseDTO;
import com.foodrush.api.dto.response.AdminUserResponseDTO;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.service.AdminService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock
  private AdminService adminService;

  @InjectMocks
  private AdminController adminController;

  @Test
  @DisplayName("GET /admin/restaurants should return 200 with restaurant list")
  void shouldReturnRestaurantsList() {
    AdminRestaurantResponseDTO dto = new AdminRestaurantResponseDTO(
        UUID.randomUUID(), "Lanches", "12345678000199", "11999998888",
        UUID.randomUUID(), "Owner", "owner@email.com",
        new BigDecimal("10.00"), true, "COL-1", OffsetDateTime.now(), true, new BigDecimal("5.00")
    );
    when(adminService.getAllRestaurants()).thenReturn(List.of(dto));

    ResponseEntity<List<AdminRestaurantResponseDTO>> response = adminController.getAllRestaurants();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().getFirst().tradeName()).isEqualTo("Lanches");
  }

  @Test
  @DisplayName("PATCH /admin/restaurants/{id}/commission should update and return 200")
  void shouldUpdateCommission() {
    UUID id = UUID.randomUUID();
    AdminUpdateCommissionRequestDTO req = new AdminUpdateCommissionRequestDTO(new BigDecimal("12.00"));
    AdminRestaurantResponseDTO dto = new AdminRestaurantResponseDTO(
        id, "Lanches", "12345678000199", "11999998888",
        UUID.randomUUID(), "Owner", "owner@email.com",
        new BigDecimal("12.00"), true, "COL-1", OffsetDateTime.now(), true, new BigDecimal("5.00")
    );
    when(adminService.updateCommissionRate(id, req)).thenReturn(dto);

    ResponseEntity<AdminRestaurantResponseDTO> response = adminController.updateCommissionRate(id, req);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().commissionRate()).isEqualByComparingTo("12.00");
    verify(adminService).updateCommissionRate(id, req);
  }

  @Test
  @DisplayName("GET /admin/users should return 200 with users list")
  void shouldReturnUsersList() {
    AdminUserResponseDTO u = new AdminUserResponseDTO(
        UUID.randomUUID(), "User", "user@email.com", "11999998888", "12345678909", RoleType.CLIENT, true
    );
    when(adminService.getAllUsers()).thenReturn(List.of(u));

    ResponseEntity<List<AdminUserResponseDTO>> response = adminController.getAllUsers();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  @DisplayName("PATCH /admin/users/{id}/toggle-status should toggle and return 200")
  void shouldToggleUserStatus() {
    UUID id = UUID.randomUUID();
    AdminUserResponseDTO u = new AdminUserResponseDTO(
        id, "User", "user@email.com", "11999998888", "12345678909", RoleType.CLIENT, false
    );
    when(adminService.toggleUserStatus(id)).thenReturn(u);

    ResponseEntity<AdminUserResponseDTO> response = adminController.toggleUserStatus(id);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().active()).isFalse();
  }

  @Test
  @DisplayName("GET /admin/metrics should return 200 with metrics")
  void shouldReturnMetrics() {
    AdminMetricsResponseDTO m = new AdminMetricsResponseDTO(
        10, 8, 50, 100, new BigDecimal("5000.00"), new BigDecimal("500.00")
    );
    when(adminService.getMetrics()).thenReturn(m);

    ResponseEntity<AdminMetricsResponseDTO> response = adminController.getMetrics();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().totalGrossVolume()).isEqualByComparingTo("5000.00");
    assertThat(response.getBody().totalPlatformFees()).isEqualByComparingTo("500.00");
  }
}
