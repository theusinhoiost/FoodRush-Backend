package com.foodrush.api.service;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.response.AddressResponseDTO;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.Address;
import com.foodrush.api.repository.AddressRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

  private final AddressRepository addressRepository;
  private final GeocodingService geocodingService;

  public AddressService(
    AddressRepository addressRepository,
    GeocodingService geocodingService
  ) {
    this.addressRepository = addressRepository;
    this.geocodingService = geocodingService;
  }

  @Transactional(readOnly = true)
  public List<AddressResponseDTO> findAll() {
    return addressRepository
      .findAll()
      .stream()
      .map(AddressResponseDTO::fromEntity)
      .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public AddressResponseDTO findById(UUID id) {
    Address address = addressRepository
      .findById(id)
      .orElseThrow(() -> new BusinessException("Endereço não encontrado."));
    return AddressResponseDTO.fromEntity(address);
  }

  @Transactional
  public AddressResponseDTO create(AddressRequestDTO dto) {
    Address resolvedAddress = geocodingService.resolveAddress(dto);
    Address savedAddress = addressRepository.save(resolvedAddress);
    return AddressResponseDTO.fromEntity(savedAddress);
  }

  public AddressResponseDTO previewGeocode(AddressRequestDTO dto) {
    Address resolvedAddress = geocodingService.resolveAddress(dto);
    return AddressResponseDTO.fromEntity(resolvedAddress);
  }
}

