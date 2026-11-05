package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.response.AddressResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Address;
import com.foodrush.api.repository.AddressRepository;
import com.opencagedata.jopencage.JOpenCageGeocoder;
import com.opencagedata.jopencage.model.JOpenCageForwardRequest;
import com.opencagedata.jopencage.model.JOpenCageLatLng;
import com.opencagedata.jopencage.model.JOpenCageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class AddressAndGeocodingTest {

  @Mock
  private AddressRepository addressRepository;

  @Mock
  private RestClient restClient;

  @Mock
  @SuppressWarnings("rawtypes")
  private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  @SuppressWarnings("rawtypes")
  private RestClient.RequestHeadersSpec requestHeadersSpec;

  @Mock
  private RestClient.ResponseSpec responseSpec;

  @Mock
  private JOpenCageGeocoder jOpenCageGeocoder;

  @Test
  @DisplayName(
    "Deve resolver endereço e coordenadas exatas via OpenCage Geocoder e gerar endereço completo e links de mapa"
  )
  @SuppressWarnings("unchecked")
  void shouldResolveCoordinatesUsingOpenCageAndFormatResponse() throws Exception {
    GeocodingService geocodingService = new GeocodingService(
      restClient,
      jOpenCageGeocoder
    );
    AddressService addressService = new AddressService(
      addressRepository,
      geocodingService
    );

    AddressRequestDTO requestDTO = new AddressRequestDTO(
      "01001-000",
      "100",
      "Loja 1"
    );

    GeocodingService.BrasilApiCepResponse brasilApiResponse =
      new GeocodingService.BrasilApiCepResponse(
        "01001000",
        "SP",
        "São Paulo",
        "Sé",
        "Praça da Sé",
        null
      );

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(
      requestHeadersSpec
    );
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(
      responseSpec.body(GeocodingService.BrasilApiCepResponse.class)
    ).thenReturn(brasilApiResponse);

    JOpenCageLatLng latLng = new JOpenCageLatLng();
    latLng.setLat(-23.550520);
    latLng.setLng(-46.633309);

    JOpenCageResponse openCageResponse = mock(JOpenCageResponse.class);
    when(openCageResponse.getFirstPosition()).thenReturn(latLng);
    when(
      jOpenCageGeocoder.forward(any(JOpenCageForwardRequest.class))
    ).thenReturn(openCageResponse);

    AddressResponseDTO response = addressService.previewGeocode(requestDTO);

    assertThat(response).isNotNull();
    assertThat(response.cep()).isEqualTo("01001000");
    assertThat(response.street()).isEqualTo("Praça da Sé");
    assertThat(response.number()).isEqualTo("100");
    assertThat(response.complement()).isEqualTo("Loja 1");
    assertThat(response.neighborhood()).isEqualTo("Sé");
    assertThat(response.city()).isEqualTo("São Paulo");
    assertThat(response.state()).isEqualTo("SP");
    assertThat(response.formattedAddress()).isEqualTo(
      "Praça da Sé, 100 - Loja 1, Sé, São Paulo - SP, CEP: 01001-000"
    );
    assertThat(response.latitude()).isEqualByComparingTo("-23.550520");
    assertThat(response.longitude()).isEqualByComparingTo("-46.633309");
    assertThat(response.mapUrl()).isEqualTo(
      "https://www.google.com/maps?q=-23.550520,-46.633309"
    );
    assertThat(response.openStreetMapUrl()).isEqualTo(
      "https://www.openstreetmap.org/?mlat=-23.550520&mlon=-46.633309#map=17/-23.550520/-46.633309"
    );

    verify(jOpenCageGeocoder, times(1)).forward(
      any(JOpenCageForwardRequest.class)
    );
  }

  @Test
  @DisplayName(
    "Deve buscar endereço por ID e retornar dados formatados e links do mapa"
  )
  void shouldFindAddressByIdSuccessfully() {
    GeocodingService geocodingService = new GeocodingService(
      restClient,
      jOpenCageGeocoder
    );
    AddressService addressService = new AddressService(
      addressRepository,
      geocodingService
    );

    UUID addressId = UUID.randomUUID();
    Address address = Address.builder()
      .id(addressId)
      .cep("01310100")
      .street("Avenida Paulista")
      .number("1578")
      .complement("Bela Vista")
      .neighborhood("Bela Vista")
      .city("São Paulo")
      .state("SP")
      .latitude(new BigDecimal("-23.561414"))
      .longitude(new BigDecimal("-46.655881"))
      .build();

    when(addressRepository.findById(addressId)).thenReturn(
      Optional.of(address)
    );

    AddressResponseDTO dto = addressService.findById(addressId);

    assertThat(dto.id()).isEqualTo(addressId);
    assertThat(dto.formattedAddress()).isEqualTo(
      "Avenida Paulista, 1578 - Bela Vista, Bela Vista, São Paulo - SP, CEP: 01310-100"
    );
    assertThat(dto.mapUrl()).isEqualTo(
      "https://www.google.com/maps?q=-23.561414,-46.655881"
    );
  }

  @Test
  @DisplayName("Deve listar todos os endereços cadastrados")
  void shouldFindAllAddresses() {
    GeocodingService geocodingService = new GeocodingService(
      restClient,
      jOpenCageGeocoder
    );
    AddressService addressService = new AddressService(
      addressRepository,
      geocodingService
    );

    Address address = Address.builder()
      .id(UUID.randomUUID())
      .cep("01001000")
      .street("Praça da Sé")
      .number("100")
      .neighborhood("Sé")
      .city("São Paulo")
      .state("SP")
      .latitude(new BigDecimal("-23.550520"))
      .longitude(new BigDecimal("-46.633309"))
      .build();

    when(addressRepository.findAll()).thenReturn(List.of(address));

    List<AddressResponseDTO> list = addressService.findAll();

    assertThat(list).hasSize(1);
    assertThat(list.getFirst().formattedAddress()).isEqualTo(
      "Praça da Sé, 100, Sé, São Paulo - SP, CEP: 01001-000"
    );
  }

  @Test
  @DisplayName("Deve lançar BusinessException quando endereço não for encontrado")
  void shouldThrowWhenAddressNotFound() {
    GeocodingService geocodingService = new GeocodingService(
      restClient,
      jOpenCageGeocoder
    );
    AddressService addressService = new AddressService(
      addressRepository,
      geocodingService
    );

    UUID unknownId = UUID.randomUUID();
    when(addressRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> addressService.findById(unknownId))
      .isInstanceOf(BusinessException.class)
      .hasMessage("Endereço não encontrado.");
  }
}
