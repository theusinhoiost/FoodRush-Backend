package com.foodrush.api.controller;

import com.foodrush.api.dto.request.AdminUpdateCommissionRequestDTO;
import com.foodrush.api.dto.response.AdminMetricsResponseDTO;
import com.foodrush.api.dto.response.AdminRestaurantResponseDTO;
import com.foodrush.api.dto.response.AdminUserResponseDTO;
import com.foodrush.api.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminService adminService;

  @GetMapping("/restaurants")
  public ResponseEntity<List<AdminRestaurantResponseDTO>> getAllRestaurants() {
    return ResponseEntity.ok(adminService.getAllRestaurants());
  }

  @PatchMapping("/restaurants/{id}/commission")
  public ResponseEntity<AdminRestaurantResponseDTO> updateCommissionRate(
      @PathVariable UUID id,
      @Valid @RequestBody AdminUpdateCommissionRequestDTO request
  ) {
    return ResponseEntity.ok(adminService.updateCommissionRate(id, request));
  }

  @GetMapping("/users")
  public ResponseEntity<List<AdminUserResponseDTO>> getAllUsers() {
    return ResponseEntity.ok(adminService.getAllUsers());
  }

  @PatchMapping("/users/{id}/toggle-status")
  public ResponseEntity<AdminUserResponseDTO> toggleUserStatus(@PathVariable UUID id) {
    return ResponseEntity.ok(adminService.toggleUserStatus(id));
  }

  @GetMapping("/metrics")
  public ResponseEntity<AdminMetricsResponseDTO> getMetrics() {
    return ResponseEntity.ok(adminService.getMetrics());
  }
}
