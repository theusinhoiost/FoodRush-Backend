package com.foodrush.api.service;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Address;
import com.opencagedata.jopencage.JOpenCageGeocoder;
import com.opencagedata.jopencage.model.JOpenCageForwardRequest;
import com.opencagedata.jopencage.model.JOpenCageLatLng;
import com.opencagedata.jopencage.model.JOpenCageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeocodingService {

  private final RestClient restClient;
  private final JOpenCageGeocoder jOpenCageGeocoder;

  @Autowired
  public GeocodingService(
    @Value("${opencage.api.key:}") String openCageApiKey
  ) {
    this.restClient = RestClient.builder()
      .defaultHeader("User-Agent", "FoodRushAPI/1.0")
      .build();
    this.jOpenCageGeocoder = (openCageApiKey != null && !openCageApiKey.isBlank())
      ? new JOpenCageGeocoder(openCageApiKey)
      : null;
  }

  public GeocodingService() {
    this("");
  }

  public GeocodingService(RestClient restClient) {
    this(restClient, null);
  }

  public GeocodingService(
    RestClient restClient,
    JOpenCageGeocoder jOpenCageGeocoder
  ) {
    this.restClient = restClient;
    this.jOpenCageGeocoder = jOpenCageGeocoder;
  }

  public Address resolveAddress(AddressRequestDTO dto) {
    String cleanCep = dto.cep().replaceAll("\\D", "");

    BrasilApiCepResponse cepData = fetchCepFromBrasilApi(cleanCep);

    BigDecimal latitude = null;
    BigDecimal longitude = null;

    // 1. Prioritize OpenCage Geocoder for exact street + number coordinate resolution when configured
    BigDecimal[] openCageCoords = fetchCoordinatesFromOpenCage(
      cepData.street(),
      dto.number(),
      cepData.neighborhood(),
      cepData.city(),
      cepData.state(),
      cleanCep
    );

    if (openCageCoords != null) {
      latitude = openCageCoords[0];
      longitude = openCageCoords[1];
    } else if (
      cepData.location() != null &&
      cepData.location().coordinates() != null &&
      cepData.location().coordinates().latitude() != null &&
      cepData.location().coordinates().longitude() != null &&
      !cepData.location().coordinates().latitude().isBlank() &&
      !cepData.location().coordinates().longitude().isBlank()
    ) {
      // 2. Fallback to BrasilAPI CEP v2 coordinates
      latitude = new BigDecimal(
        cepData.location().coordinates().latitude()
      ).setScale(6, RoundingMode.HALF_UP);
      longitude = new BigDecimal(
        cepData.location().coordinates().longitude()
      ).setScale(6, RoundingMode.HALF_UP);
    } else {
      latitude = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
      longitude = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }

    return Address.builder()
      .cep(cleanCep)
      .street(cepData.street() != null ? cepData.street() : "N/A")
      .number(dto.number())
      .complement(dto.complement())
      .neighborhood(
        cepData.neighborhood() != null ? cepData.neighborhood() : "Centro"
      )
      .city(cepData.city())
      .state(cepData.state())
      .latitude(latitude)
      .longitude(longitude)
      .build();
  }

  private BrasilApiCepResponse fetchCepFromBrasilApi(String cleanCep) {
    try {
      BrasilApiCepResponse response = restClient
        .get()
        .uri("https://brasilapi.com.br/api/cep/v2/{cep}", cleanCep)
        .retrieve()
        .body(BrasilApiCepResponse.class);

      if (response == null || response.city() == null) {
        throw new BusinessException(
          "CEP não encontrado ou inválido: " + cleanCep
        );
      }
      return response;
    } catch (RestClientException e) {
      throw new BusinessException(
        "Não foi possível localizar o CEP informado: " + cleanCep
      );
    }
  }

  private BigDecimal[] fetchCoordinatesFromOpenCage(
    String street,
    String number,
    String neighborhood,
    String city,
    String state,
    String cep
  ) {
    if (jOpenCageGeocoder == null) {
      return null;
    }
    try {
      String query = String.format(
        "%s, %s, %s, %s - %s, %s, Brazil",
        street != null ? street : "",
        number != null ? number : "",
        neighborhood != null ? neighborhood : "",
        city != null ? city : "",
        state != null ? state : "",
        cep != null ? cep : ""
      );

      JOpenCageForwardRequest request = new JOpenCageForwardRequest(query);
      request.setRestrictToCountryCode("br");
      request.setLanguage("pt");
      request.setLimit(1);
      request.setNoAnnotations(false);

      JOpenCageResponse response = jOpenCageGeocoder.forward(request);
      if (response != null) {
        JOpenCageLatLng firstPosition = response.getFirstPosition();
        if (
          firstPosition != null &&
          firstPosition.getLat() != null &&
          firstPosition.getLng() != null
        ) {
          return new BigDecimal[] {
            BigDecimal.valueOf(firstPosition.getLat()).setScale(
              6,
              RoundingMode.HALF_UP
            ),
            BigDecimal.valueOf(firstPosition.getLng()).setScale(
              6,
              RoundingMode.HALF_UP
            ),
          };
        }
      }
    } catch (Exception ignored) {
      // Silent fallback to BrasilAPI coordinates if OpenCage is unavailable or quota is exceeded
    }
    return null;
  }

  public record BrasilApiCepResponse(
    String cep,
    String state,
    String city,
    String neighborhood,
    String street,
    LocationData location
  ) {}

  public record LocationData(String type, CoordinatesData coordinates) {}

  public record CoordinatesData(String longitude, String latitude) {}
}

