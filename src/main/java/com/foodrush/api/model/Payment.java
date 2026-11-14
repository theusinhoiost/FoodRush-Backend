package com.foodrush.api.model;

import com.foodrush.api.enums.PaymentMethod;
import com.foodrush.api.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "external_id", length = 100)
  private String externalId;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", length = 30, nullable = false)
  private PaymentMethod paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30, nullable = false)
  private PaymentStatus status;

  @Column(name = "amount", precision = 10, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "qr_code", columnDefinition = "TEXT")
  private String qrCode;

  @Column(name = "qr_code_base64", columnDefinition = "TEXT")
  private String qrCodeBase64;

  @Column(name = "ticket_url", length = 500)
  private String ticketUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
