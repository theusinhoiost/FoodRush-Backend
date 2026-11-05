package com.foodrush.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class implementing the Haversine formula to determine the great-circle
 * distance between two geographical points given their latitude and longitude coordinates.
 */
public final class GeoDistanceUtils {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private GeoDistanceUtils() {
    // Utility class
  }

  /**
   * Calculates the distance in kilometers between two geographic coordinates.
   *
   * @param lat1 Latitude of first point
   * @param lon1 Longitude of first point
   * @param lat2 Latitude of second point
   * @param lon2 Longitude of second point
   * @return Distance in kilometers, rounded to 2 decimal places
   */
  public static double calculateDistanceKm(
      BigDecimal lat1,
      BigDecimal lon1,
      BigDecimal lat2,
      BigDecimal lon2
  ) {
    if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
      throw new IllegalArgumentException("Latitude and longitude values cannot be null");
    }

    double lat1Rad = Math.toRadians(lat1.doubleValue());
    double lon1Rad = Math.toRadians(lon1.doubleValue());
    double lat2Rad = Math.toRadians(lat2.doubleValue());
    double lon2Rad = Math.toRadians(lon2.doubleValue());

    double dLat = lat2Rad - lat1Rad;
    double dLon = lon2Rad - lon1Rad;

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(lat1Rad) * Math.cos(lat2Rad)
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    double distance = EARTH_RADIUS_KM * c;

    return BigDecimal.valueOf(distance)
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }
}

