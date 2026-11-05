package com.foodrush.api.controller;

import com.foodrush.api.dto.request.AddressRequestDTO;
import com.foodrush.api.dto.response.AddressResponseDTO;
import com.foodrush.api.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/addresses")
@Tag(
  name = "Addresses",
  description = "Endpoints for resolving geographic coordinates (latitude/longitude) via OpenCage Geocoder & BrasilAPI, displaying formatted full addresses, and providing map location links"
)
public class AddressController {

  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @GetMapping
  @Operation(
    summary = "List all registered addresses",
    description = "Retrieves all saved addresses including their full formatted display text, geographic coordinates (latitude/longitude), and direct map links.",
    security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "List of addresses retrieved successfully",
        content = @Content(
          mediaType = "application/json",
          array = @ArraySchema(
            schema = @Schema(implementation = AddressResponseDTO.class)
          )
        )
      ),
    }
  )
  public ResponseEntity<List<AddressResponseDTO>> findAll() {
    return ResponseEntity.ok(addressService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(
    summary = "Get address by ID",
    description = "Retrieves complete address details, formatted display text, geographic coordinates, and map visualization URLs by its unique UUID.",
    security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Address found and returned successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = AddressResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "422",
        description = "Address not found with the specified ID",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<AddressResponseDTO> findById(
    @Parameter(
      description = "Unique UUID of the address",
      example = "a0000000-0000-0000-0000-000000000001",
      required = true
    ) @PathVariable UUID id
  ) {
    return ResponseEntity.ok(addressService.findById(id));
  }

  @PostMapping
  @Operation(
    summary = "Resolve, geocode, and save a new address",
    description = "Resolves street details from the CEP, fetches geographic coordinates (latitude and longitude) using OpenCage Geocoder, saves the address, and returns the complete formatted address along with map URLs.",
    security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "201",
        description = "Address resolved and saved successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = AddressResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload or validation failure",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
      @ApiResponse(
        responseCode = "422",
        description = "CEP not found or invalid",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<AddressResponseDTO> create(
    @RequestBody @Valid AddressRequestDTO dto
  ) {
    AddressResponseDTO created = addressService.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PostMapping("/geocode")
  @Operation(
    summary = "Preview geocoded address and map links without saving",
    description = "Resolves the full street address, latitude/longitude coordinates via OpenCage Geocoder, and map URLs from CEP and street number without persisting to the database.",
    security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Address geocoded successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = AddressResponseDTO.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload or validation failure",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
      @ApiResponse(
        responseCode = "422",
        description = "CEP not found or invalid",
        content = @Content(
          mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)
        )
      ),
    }
  )
  public ResponseEntity<AddressResponseDTO> previewGeocode(
    @RequestBody @Valid AddressRequestDTO dto
  ) {
    return ResponseEntity.ok(addressService.previewGeocode(dto));
  }
}

