package com.foodrush.api.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MercadoPagoConfiguration {

  @Value("${mercadopago.access-token:}")
  private String accessToken;

  @PostConstruct
  public void init() {
    if (accessToken != null && !accessToken.isBlank()) {
      MercadoPagoConfig.setAccessToken(accessToken);
      log.info("Mercado Pago SDK successfully initialized with configured Access Token");
    } else {
      log.warn("Mercado Pago Access Token is not set. Payment calls will require configured credentials.");
    }
  }

  @Bean
  public PaymentClient paymentClient() {
    return new PaymentClient();
  }
}
