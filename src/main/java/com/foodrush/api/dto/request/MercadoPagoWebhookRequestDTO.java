package com.foodrush.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Mercado Pago Webhook notification payload")
public record MercadoPagoWebhookRequestDTO(
  @Schema(description = "Event action name", example = "payment.updated")
  String action,

  @Schema(description = "Resource event type", example = "payment")
  String type,

  @Schema(description = "Resource identification wrapper")
  WebhookData data,

  @JsonProperty("date_created")
  @Schema(description = "Event occurrence date timestamp")
  String dateCreated,

  @Schema(description = "Notification ID", example = "9988776655")
  Long id,

  @JsonProperty("live_mode")
  @Schema(description = "Whether the event happened in live production mode", example = "false")
  Boolean liveMode,

  @JsonProperty("user_id")
  @Schema(description = "Mercado Pago user account ID", example = "123456789")
  Long userId
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Schema(description = "Data reference inside webhook notification")
  public record WebhookData(
    @Schema(description = "Target entity ID (e.g. Payment ID)", example = "12345678901")
    String id
  ) {}
}
