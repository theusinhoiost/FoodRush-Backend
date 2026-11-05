package com.foodrush.api.repository;

import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
  Optional<Order> findByIdempotencyKey(String idempotencyKey);

  boolean existsByIdempotencyKey(String idempotencyKey);

  List<Order> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<Order> findAllByRestaurantIdAndStatus(
    UUID restaurantId,
    OrderStatus status
  );

  @Query(
    "SELECT o FROM Order o " +
      "LEFT JOIN FETCH o.items i " +
      "LEFT JOIN FETCH i.complements " +
      "WHERE o.id = :id"
  )
  Optional<Order> findByIdWithDetails(@Param("id") UUID id);

  long countByStatus(OrderStatus status);
}
