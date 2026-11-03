package com.foodrush.api.service;

import com.foodrush.api.dto.response.DeliveryFeeEstimateResponseDTO;
import com.foodrush.api.dto.response.RestaurantResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Restaurant;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.util.GeoDistanceUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCalculationService {

  public static final BigDecimal DEFAULT_FEE_PER_KM = new BigDecimal("1.50");
  public static final int BASE_PREPARATION_MINUTES = 20;
  public static final int MINUTES_PER_KM = 4;

  private final RestaurantRepository restaurantRepository;

  /**
   * Calculates dynamic delivery fee and estimated delivery duration based on Haversine distance.
   */
  public DeliveryFeeEstimateResponseDTO calculateEstimate(
      BigDecimal originLat,
      BigDecimal originLon,
      Restaurant restaurant
  ) {
    if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
      throw new BusinessException("Restaurant coordinates are not configured");
    }

    double distanceKm = GeoDistanceUtils.calculateDistanceKm(
        originLat,
        originLon,
        restaurant.getLatitude(),
        restaurant.getLongitude()
    );

    BigDecimal baseFee = restaurant.getBaseDeliveryFee() != null
        ? restaurant.getBaseDeliveryFee()
        : BigDecimal.ZERO;

    BigDecimal distanceFee = DEFAULT_FEE_PER_KM.multiply(BigDecimal.valueOf(distanceKm));
    BigDecimal totalDeliveryFee = baseFee.add(distanceFee).setScale(2, RoundingMode.HALF_UP);

    int estimatedMinutes = BASE_PREPARATION_MINUTES + (int) Math.ceil(distanceKm * MINUTES_PER_KM);

    return new DeliveryFeeEstimateResponseDTO(
        restaurant.getId(),
        restaurant.getTradeName(),
        distanceKm,
        baseFee,
        DEFAULT_FEE_PER_KM,
        totalDeliveryFee,
        estimatedMinutes
    );
  }

  @Transactional(readOnly = true)
  public DeliveryFeeEstimateResponseDTO estimateForRestaurant(
      UUID restaurantId,
      BigDecimal customerLat,
      BigDecimal customerLon
  ) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
        .orElseThrow(() -> new BusinessException("Restaurant not found with ID: " + restaurantId));

    return calculateEstimate(customerLat, customerLon, restaurant);
  }

  @Transactional(readOnly = true)
  public List<RestaurantResponseDTO> findNearbyRestaurants(
      BigDecimal customerLat,
      BigDecimal customerLon,
      double radiusKm
  ) {
    if (customerLat == null || customerLon == null) {
      throw new BusinessException("Customer coordinates are mandatory for nearby search");
    }

    return restaurantRepository.findAll()
        .stream()
        .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
        .filter(r -> {
          double dist = GeoDistanceUtils.calculateDistanceKm(
              customerLat,
              customerLon,
              r.getLatitude(),
              r.getLongitude()
          );
          return dist <= radiusKm;
        })
        .map(RestaurantResponseDTO::fromEntity)
        .toList();
  }
}

