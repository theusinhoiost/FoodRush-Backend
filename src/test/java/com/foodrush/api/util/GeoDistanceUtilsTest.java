package com.foodrush.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoDistanceUtilsTest {

  @Test
  @DisplayName("Deve calcular distância correta de São Paulo Sé até a Av Paulista (aprox. 2.7km)")
  void shouldCalculateDistanceBetweenSeAndPaulista() {
    // Praça da Sé: -23.550520, -46.633308
    BigDecimal lat1 = new BigDecimal("-23.550520");
    BigDecimal lon1 = new BigDecimal("-46.633308");

    // MASP (Av Paulista): -23.561414, -46.655881
    BigDecimal lat2 = new BigDecimal("-23.561414");
    BigDecimal lon2 = new BigDecimal("-46.655881");

    double distanceKm = GeoDistanceUtils.calculateDistanceKm(lat1, lon1, lat2, lon2);

    // Distância linear real é de aproximadamente 2.60 a 2.75 km
    assertThat(distanceKm).isBetween(2.5, 2.9);
  }

  @Test
  @DisplayName("Deve retornar 0.0 quando as coordenadas forem idênticas")
  void shouldReturnZeroForIdenticalCoordinates() {
    BigDecimal lat = new BigDecimal("-23.550520");
    BigDecimal lon = new BigDecimal("-46.633308");

    double distance = GeoDistanceUtils.calculateDistanceKm(lat, lon, lat, lon);

    assertThat(distance).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Deve lançar IllegalArgumentException quando coordenadas forem nulas")
  void shouldThrowWhenCoordinatesAreNull() {
    BigDecimal lat = new BigDecimal("-23.550520");

    assertThatThrownBy(() -> GeoDistanceUtils.calculateDistanceKm(lat, null, lat, lat))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

