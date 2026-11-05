package com.foodrush.api.controller;

import com.foodrush.api.dto.request.MenuItemRequestDTO;
import com.foodrush.api.dto.response.MenuItemResponseDTO;
import com.foodrush.api.service.MenuService;
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
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(
    name = "Menu",
    description = "Endpoints for exploring restaurant menus, managing dishes, sizes, and complement customization options"
)
public class MenuController {

  private final MenuService menuService;

  @GetMapping("/restaurants/{restaurantId}/menu")
  @Operation(
      summary = "Get full restaurant menu",
      description = "Retrieves the complete catalog of active dishes for a restaurant, including available sizes, stock inventory, and complement groups."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Menu retrieved successfully",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = MenuItemResponseDTO.class))
              )
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Restaurant not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<List<MenuItemResponseDTO>> getRestaurantMenu(
      @Parameter(description = "Unique UUID of the restaurant", required = true)
      @PathVariable UUID restaurantId
  ) {
    List<MenuItemResponseDTO> menu = menuService.findMenuByRestaurant(restaurantId);
    return ResponseEntity.ok(menu);
  }

  @GetMapping("/menu-items/{id}")
  @Operation(
      summary = "Get menu item by ID",
      description = "Retrieves individual dish details, size variations, and complement groups."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Menu item found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = MenuItemResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Menu item not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<MenuItemResponseDTO> getMenuItemById(
      @Parameter(description = "Unique UUID of the menu item", required = true)
      @PathVariable UUID id
  ) {
    MenuItemResponseDTO item = menuService.findById(id);
    return ResponseEntity.ok(item);
  }

  @PostMapping("/restaurants/{restaurantId}/menu-items")
  @Operation(
      summary = "Add a new dish to restaurant menu",
      description = "Creates a new menu item belonging to the specified restaurant along with sizes and complement groups.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "201",
              description = "Menu item created successfully",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = MenuItemResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Validation error on payload",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Restaurant not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<MenuItemResponseDTO> createMenuItem(
      @Parameter(description = "Unique UUID of the restaurant", required = true)
      @PathVariable UUID restaurantId,
      @RequestBody @Valid MenuItemRequestDTO dto,
      Principal principal
  ) {
    String email = principal != null ? principal.getName() : null;
    MenuItemResponseDTO created = menuService.create(restaurantId, dto, email);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/menu-items/{id}/toggle-available")
  @Operation(
      summary = "Toggle dish availability",
      description = "Enables or disables visibility of a dish on the public menu.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Availability toggled successfully",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = MenuItemResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Menu item not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<MenuItemResponseDTO> toggleAvailable(
      @Parameter(description = "Unique UUID of the menu item", required = true)
      @PathVariable UUID id,
      Principal principal
  ) {
    String email = principal != null ? principal.getName() : null;
    MenuItemResponseDTO updated = menuService.toggleAvailability(id, email);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/menu-items/{id}")
  @Operation(
      summary = "Delete menu item",
      description = "Permanently removes a menu item from the restaurant catalog.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "204",
              description = "Menu item deleted successfully"
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Menu item not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<Void> deleteMenuItem(
      @Parameter(description = "Unique UUID of the menu item", required = true)
      @PathVariable UUID id,
      Principal principal
  ) {
    String email = principal != null ? principal.getName() : null;
    menuService.delete(id, email);
    return ResponseEntity.noContent().build();
  }
}

