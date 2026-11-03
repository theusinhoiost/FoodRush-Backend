package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.foodrush.api.dto.response.DeliveryFeeEstimateResponseDTO;
import com.foodrush.api.dto.response.RestaurantResponseDTO;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.model.User;
import com.foodrush.api.repository.RestaurantRepository;
import java.math.BigDecimal;
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
class DeliveryCalculationServiceTest {

  @Mock
  private RestaurantRepository restaurantRepository;

  @InjectMocks
  private DeliveryCalculationService deliveryCalculationService;

  @Test
  @DisplayName("Deve calcular estimativa de frete e tempo com base na fórmula de Haversine")
  void shouldCalculateEstimateCorrectly() {
    UUID restaurantId = UUID.randomUUID();
    Restaurant restaurant = new Restaurant();
    restaurant.setId(restaurantId);
    restaurant.setTradeName("Burger Rush Paulista");
    restaurant.setBaseDeliveryFee(new BigDecimal("6.00"));
    restaurant.setLatitude(new BigDecimal("-23.561414")); // MASP
    restaurant.setLongitude(new BigDecimal("-46.655881"));

    when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

    // Cliente na Praça da Sé: -23.550520, -46.633308 (distância ~2.65km)
    BigDecimal clientLat = new BigDecimal("-23.550520");
    BigDecimal clientLon = new BigDecimal("-46.633308");

    DeliveryFeeEstimateResponseDTO result = deliveryCalculationService.estimateForRestaurant(
        restaurantId, clientLat, clientLon
    );

    assertThat(result).isNotNull();
    assertThat(result.restaurantName()).isEqualTo("Burger Rush Paulista");
    assertThat(result.distanceKm()).isBetween(2.5, 2.9);
    assertThat(result.baseFee()).isEqualByComparingTo("6.00");
    assertThat(result.feePerKm()).isEqualByComparingTo("1.50");
    // total = baseFee (6.00) + 1.50 * dist (~4.00) = ~10.00
    assertThat(result.totalDeliveryFee()).isGreaterThan(new BigDecimal("9.50"));
    assertThat(result.estimatedMinutes()).isGreaterThanOrEqualTo(30);
  }

  @Test
  @DisplayName("Deve filtrar apenas restaurantes dentro do raio especificado")
  void shouldFilterRestaurantsWithinRadius() {
    User mockOwner = new User();
    mockOwner.setId(UUID.randomUUID());

    // Restaurante 1: Perto (~2.7km da Sé)
    Restaurant near = new Restaurant();
    near.setId(UUID.randomUUID());
    near.setOwner(mockOwner);
    near.setTradeName("Restaurante Perto");
    near.setLatitude(new BigDecimal("-23.561414"));
    near.setLongitude(new BigDecimal("-46.655881"));
    near.setIsOpen(true);

    // Restaurante 2: Campinas (~90km de SP)
    Restaurant far = new Restaurant();
    far.setId(UUID.randomUUID());
    far.setOwner(mockOwner);
    far.setTradeName("Restaurante Longe");
    far.setLatitude(new BigDecimal("-22.909938"));
    far.setLongitude(new BigDecimal("-47.062633"));
    far.setIsOpen(true);

    when(restaurantRepository.findAll()).thenReturn(List.of(near, far));

    BigDecimal clientLat = new BigDecimal("-23.550520"); // Sé
    BigDecimal clientLon = new BigDecimal("-46.633308");

    List<RestaurantResponseDTO> nearby = deliveryCalculationService.findNearbyRestaurants(
        clientLat, clientLon, 5.0 // raio de 5km
    );

    assertThat(nearby).hasSize(1);
    assertThat(nearby.get(0).tradeName()).isEqualTo("Restaurante Perto");
  }
}
