package com.foodrush.api.controller;

import com.foodrush.api.dto.request.MercadoPagoWebhookRequestDTO;
import com.foodrush.api.dto.response.PaymentResponseDTO;
import com.foodrush.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(
    name = "Payments",
    description = "Endpoints for generating Pix QR codes with Mercado Pago and handling webhook status notifications"
)
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/orders/{orderId}/pix")
  @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
  @Operation(
      summary = "Generate Pix Payment for an Order",
      description = "Generates a Mercado Pago Pix QR code (base64) and Pix Copia e Cola payload for an order in CREATED state.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Pix payment generated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Order is not in valid status or payment failed",
          content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "User is not owner of the order",
          content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Order not found",
          content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<PaymentResponseDTO> createPixPayment(
      @PathVariable UUID orderId,
      Principal principal
  ) {
    PaymentResponseDTO response = paymentService.createPixPayment(orderId, principal.getName());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/orders/{orderId}")
  @Operation(
      summary = "Get Payment Details by Order ID",
      description = "Retrieves current payment status and Pix QR codes for a specific order.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "User is not authorized to view payment for this order",
          content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Order or payment not found",
          content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<PaymentResponseDTO> getPaymentByOrderId(
      @PathVariable UUID orderId,
      Principal principal,
      Authentication authentication
  ) {
    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    PaymentResponseDTO response = paymentService.getPaymentByOrderId(orderId, principal.getName(), isAdmin);
    return ResponseEntity.ok(response);
  }

  @RequestMapping(value = "/webhook", method = {RequestMethod.POST, RequestMethod.GET})
  @Operation(
      summary = "Mercado Pago Webhook",
      description = "Receives asynchronous payment status update notifications sent by Mercado Pago. Public endpoint."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notification received and processed successfully")
  })
  public ResponseEntity<Void> handleWebhook(
      @RequestBody(required = false) MercadoPagoWebhookRequestDTO payload,
      @Parameter(description = "Topic or event type from query parameter") @RequestParam(required = false) String topic,
      @Parameter(description = "Resource ID from query parameter") @RequestParam(required = false) String id,
      @Parameter(description = "Data ID from query parameter") @RequestParam(value = "data.id", required = false) String dataId
  ) {
    String resolvedId = dataId != null && !dataId.isBlank() ? dataId : id;
    paymentService.processWebhookNotification(topic, resolvedId, payload);
    return ResponseEntity.ok().build();
  }
}
