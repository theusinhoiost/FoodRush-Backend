package com.foodrush.api.model;




import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_size_id")
    private MenuItemSize size;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemComplement> complements = new ArrayList<>();

    public OrderItem() {
    }

    @Builder
    public OrderItem(UUID id, Order order, MenuItem menuItem, MenuItemSize size, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice, List<OrderItemComplement> complements) {
        this.id = id;
        this.order = order;
        this.menuItem = menuItem;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.complements = complements != null ? complements : new ArrayList<>();
    }

    public static class OrderItemBuilder {
        public OrderItemBuilder() {
        }
    }
}