package com.foodrush.api.dto.response;

import com.foodrush.api.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(
  description = "Complete address details including street information, formatted display string, geographic coordinates, and map visualization URLs resolved by the backend"
)
public record AddressResponseDTO(
  @Schema(
    description = "Unique identifier of the address",
    example = "a0000000-0000-0000-0000-000000000002"
  )
  UUID id,

  @Schema(description = "8-digit Brazilian postal code (CEP)", example = "01001000")
  String cep,

  @Schema(description = "Street name", example = "Praça da Sé")
  String street,

  @Schema(description = "Street number", example = "100")
  String number,

  @Schema(description = "Address complement", example = "Loja 1")
  String complement,

  @Schema(description = "Neighborhood / district", example = "Sé")
  String neighborhood,

  @Schema(description = "City name", example = "São Paulo")
  String city,

  @Schema(description = "Two-letter state code (UF)", example = "SP")
  String state,

  @Schema(
    description = "Complete formatted address for user display",
    example = "Praça da Sé, 100 - Loja 1, Sé, São Paulo - SP, CEP: 01001-000"
  )
  String formattedAddress,

  @Schema(description = "Resolved geographic latitude", example = "-23.550520")
  BigDecimal latitude,

  @Schema(description = "Resolved geographic longitude", example = "-46.633309")
  BigDecimal longitude,

  @Schema(
    description = "Direct URL to view the exact location on Google Maps",
    example = "https://www.google.com/maps?q=-23.550520,-46.633309"
  )
  String mapUrl,

  @Schema(
    description = "Direct URL to view the exact location on OpenStreetMap",
    example = "https://www.openstreetmap.org/?mlat=-23.550520&mlon=-46.633309#map=17/-23.550520/-46.633309"
  )
  String openStreetMapUrl
) implements Serializable {
  public AddressResponseDTO(
    UUID id,
    String cep,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    BigDecimal latitude,
    BigDecimal longitude
  ) {
    this(
      id,
      cep,
      street,
      number,
      complement,
      neighborhood,
      city,
      state,
      buildFormattedAddress(street, number, complement, neighborhood, city, state, cep),
      latitude,
      longitude,
      buildGoogleMapsUrl(latitude, longitude),
      buildOpenStreetMapUrl(latitude, longitude)
    );
  }

  public static AddressResponseDTO fromEntity(Address address) {
    if (address == null) {
      return null;
    }
    return new AddressResponseDTO(
      address.getId(),
      address.getCep(),
      address.getStreet(),
      address.getNumber(),
      address.getComplement(),
      address.getNeighborhood(),
      address.getCity(),
      address.getState(),
      buildFormattedAddress(
        address.getStreet(),
        address.getNumber(),
        address.getComplement(),
        address.getNeighborhood(),
        address.getCity(),
        address.getState(),
        address.getCep()
      ),
      address.getLatitude(),
      address.getLongitude(),
      buildGoogleMapsUrl(address.getLatitude(), address.getLongitude()),
      buildOpenStreetMapUrl(address.getLatitude(), address.getLongitude())
    );
  }

  private static String buildFormattedAddress(
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    String cep
  ) {
    StringBuilder sb = new StringBuilder();
    if (street != null && !street.isBlank()) {
      sb.append(street);
    }
    if (number != null && !number.isBlank()) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append(number);
    }
    if (complement != null && !complement.isBlank()) {
      sb.append(" - ").append(complement);
    }
    if (neighborhood != null && !neighborhood.isBlank()) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append(neighborhood);
    }
    if (city != null && !city.isBlank()) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append(city);
    }
    if (state != null && !state.isBlank()) {
      sb.append(" - ").append(state);
    }
    if (cep != null && !cep.isBlank()) {
      String formattedCep = cep.length() == 8
        ? cep.substring(0, 5) + "-" + cep.substring(5)
        : cep;
      sb.append(", CEP: ").append(formattedCep);
    }
    return sb.toString();
  }

  private static String buildGoogleMapsUrl(BigDecimal latitude, BigDecimal longitude) {
    if (latitude == null || longitude == null) {
      return null;
    }
    return String.format("https://www.google.com/maps?q=%s,%s", latitude.toPlainString(), longitude.toPlainString());
  }

  private static String buildOpenStreetMapUrl(BigDecimal latitude, BigDecimal longitude) {
    if (latitude == null || longitude == null) {
      return null;
    }
    String lat = latitude.toPlainString();
    String lon = longitude.toPlainString();
    return String.format("https://www.openstreetmap.org/?mlat=%s&mlon=%s#map=17/%s/%s", lat, lon, lat, lon);
  }
}

