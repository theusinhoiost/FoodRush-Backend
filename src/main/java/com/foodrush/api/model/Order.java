package com.foodrush.api.model;

import com.foodrush.api.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "restaurant_id", nullable = false)
  private Restaurant restaurant;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30, nullable = false)
  private OrderStatus status;

  @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
  private BigDecimal subtotal;

  @Column(name = "delivery_fee", precision = 10, scale = 2, nullable = false)
  private BigDecimal deliveryFee;

  @Column(name = "total", precision = 10, scale = 2, nullable = false)
  private BigDecimal total;

  @Column(
    name = "delivery_address",
    columnDefinition = "TEXT",
    nullable = false
  )
  private String deliveryAddress;

  @Column(name = "idempotency_key", length = 100, unique = true)
  private String idempotencyKey;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @BatchSize(size = 50)
  @OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<OrderItem> items = new ArrayList<>();

  public Order() {}

  @Builder
  public Order(
    UUID id,
    User customer,
    Restaurant restaurant,
    OrderStatus status,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal total,
    String deliveryAddress,
    String idempotencyKey,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<OrderItem> items
  ) {
    this.id = id;
    this.customer = customer;
    this.restaurant = restaurant;
    this.status = status;
    this.subtotal = subtotal;
    this.deliveryFee = deliveryFee;
    this.total = total;
    this.deliveryAddress = deliveryAddress;
    this.idempotencyKey = idempotencyKey;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.items = items != null ? items : new ArrayList<>();
  }

  public static class OrderBuilder {

    public OrderBuilder() {}
  }
}
