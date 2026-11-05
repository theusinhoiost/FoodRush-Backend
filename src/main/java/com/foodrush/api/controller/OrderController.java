package com.foodrush.api.controller;

import com.foodrush.api.dto.request.OrderCreateRequestDTO;
import com.foodrush.api.dto.request.OrderStatusUpdateRequestDTO;
import com.foodrush.api.dto.response.OrderResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.service.OrderService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(
    name = "Orders",
    description = "Endpoints for placing, tracking, and updating lifecycle status of delivery orders"
)
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @Operation(
      summary = "Place a new order",
      description = "Creates a new delivery order, verifies item prices, validates size stock with pessimistic locking, calculates subtotal and delivery fees, and saves order items with complement add-ons.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "201",
              description = "Order placed successfully",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = OrderResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Invalid order payload or validation failure",
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
              description = "Business rule violation (e.g. restaurant closed, insufficient size stock, or inactive user)",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<OrderResponseDTO> create(@RequestBody @Valid OrderCreateRequestDTO dto) {
    OrderResponseDTO response = orderService.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get order by ID",
      description = "Retrieves full details of an order including ordered items, sizes, and complement add-ons.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Order details retrieved successfully",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = OrderResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Order not found with the specified ID",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<OrderResponseDTO> findById(
      @Parameter(description = "Unique UUID of the order", required = true)
      @PathVariable UUID id
  ) {
    OrderResponseDTO response = orderService.findById(id);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/customer/{customerId}")
  @Operation(
      summary = "List customer orders",
      description = "Retrieves order history for a specific customer sorted by creation date descending.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "List of orders placed by customer",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = OrderResponseDTO.class))
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
      }
  )
  public ResponseEntity<List<OrderResponseDTO>> findByCustomer(
      @Parameter(description = "UUID of the customer", required = true)
      @PathVariable UUID customerId
  ) {
    List<OrderResponseDTO> orders = orderService.findAllByCustomerId(customerId);
    return ResponseEntity.ok(orders);
  }

  @GetMapping("/restaurant/{restaurantId}")
  @Operation(
      summary = "List restaurant orders by status",
      description = "Retrieves orders for a restaurant filtered by their operational status (e.g., PAID, PREPARING, READY).",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "List of matching orders for the restaurant",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = OrderResponseDTO.class))
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Missing or invalid JWT Bearer token",
              content = @Content
          ),
      }
  )
  public ResponseEntity<List<OrderResponseDTO>> findByRestaurantAndStatus(
      @Parameter(description = "UUID of the restaurant", required = true)
      @PathVariable UUID restaurantId,
      @Parameter(description = "Lifecycle status filter", required = true)
      @RequestParam OrderStatus status
  ) {
    List<OrderResponseDTO> orders = orderService.findAllByRestaurantIdAndStatus(restaurantId, status);
    return ResponseEntity.ok(orders);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Update order status",
      description = "Advances the order lifecycle (e.g. from PAID to PREPARING, READY to IN_ROUTE) and publishes an event to RabbitMQ for kitchen and courier dispatch.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Order status updated and event published",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = OrderResponseDTO.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Validation failure or missing status in payload",
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
              description = "Order not found or invalid status transition",
              content = @Content(
                  mediaType = "application/problem+json",
                  schema = @Schema(implementation = ProblemDetail.class)
              )
          ),
      }
  )
  public ResponseEntity<OrderResponseDTO> updateStatus(
      @Parameter(description = "Unique UUID of the order", required = true)
      @PathVariable UUID id,
      @RequestBody @Valid OrderStatusUpdateRequestDTO dto
  ) {
    OrderResponseDTO response = orderService.updateStatus(id, dto.status());
    return ResponseEntity.ok(response);
  }
}

