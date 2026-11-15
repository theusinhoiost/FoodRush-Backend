package com.foodrush.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Restaurant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "trade_name", length = 120, nullable = false)
  private String tradeName;

  @Column(name = "cnpj", length = 14, nullable = false, unique = true)
  private String cnpj;

  @Column(name = "phone", length = 20, nullable = false)
  private String phone;

  @Column(
    name = "base_delivery_fee",
    precision = 10,
    scale = 2,
    nullable = false
  )
  private BigDecimal baseDeliveryFee;

  @Column(name = "latitude", precision = 9, scale = 6, nullable = false)
  private BigDecimal latitude;

  @Column(name = "longitude", precision = 9, scale = 6, nullable = false)
  private BigDecimal longitude;

  @OneToOne(
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @JoinColumn(name = "address_id")
  private Address address;

  @Column(name = "is_open", nullable = false)
  private Boolean isOpen = false;

  @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
  private BigDecimal commissionRate = new BigDecimal("10.00");

  @Column(name = "mp_collector_id", length = 100)
  private String mpCollectorId;

  @Column(name = "mp_access_token", columnDefinition = "TEXT")
  private String mpAccessToken;

  @Column(name = "mp_refresh_token", columnDefinition = "TEXT")
  private String mpRefreshToken;

  @Column(name = "mp_connected", nullable = false)
  private Boolean mpConnected = false;

  @Column(name = "mp_connected_at")
  private java.time.OffsetDateTime mpConnectedAt;

  @OneToMany(
    mappedBy = "restaurant",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<MenuItem> menuItems = new ArrayList<>();

  public Restaurant() {}

  @Builder
  public Restaurant(
    UUID id,
    User owner,
    String tradeName,
    String cnpj,
    String phone,
    BigDecimal baseDeliveryFee,
    BigDecimal latitude,
    BigDecimal longitude,
    Address address,
    Boolean isOpen,
    BigDecimal commissionRate,
    String mpCollectorId,
    String mpAccessToken,
    String mpRefreshToken,
    Boolean mpConnected,
    java.time.OffsetDateTime mpConnectedAt,
    List<MenuItem> menuItems
  ) {
    this.id = id;
    this.owner = owner;
    this.tradeName = tradeName;
    this.cnpj = cnpj;
    this.phone = phone;
    this.baseDeliveryFee = baseDeliveryFee;
    this.latitude = latitude;
    this.longitude = longitude;
    this.address = address;
    this.isOpen = isOpen != null ? isOpen : false;
    this.commissionRate = commissionRate != null ? commissionRate : new BigDecimal("10.00");
    this.mpCollectorId = mpCollectorId;
    this.mpAccessToken = mpAccessToken;
    this.mpRefreshToken = mpRefreshToken;
    this.mpConnected = mpConnected != null ? mpConnected : false;
    this.mpConnectedAt = mpConnectedAt;
    this.menuItems = menuItems != null ? menuItems : new ArrayList<>();
  }

  public static class RestaurantBuilder {

    public RestaurantBuilder() {}
  }
}
