package com.foodrush.api.repository;

import com.foodrush.api.model.MenuItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
  List<MenuItem> findAllByRestaurantIdAndAvailableTrue(UUID restaurantId);

  List<MenuItem> findAllByRestaurantId(UUID restaurantId);

  @Query(
    "SELECT DISTINCT m FROM MenuItem m " +
      "LEFT JOIN FETCH m.sizes " +
      "WHERE m.id = :id AND m.available = true"
  )
  Optional<MenuItem> findByIdWithSizes(@Param("id") UUID id);

  @Query(
    "SELECT DISTINCT m FROM MenuItem m " +
      "LEFT JOIN FETCH m.complementGroups cg " +
      "WHERE m.id = :id AND m.available = true"
  )
  Optional<MenuItem> findByIdWithComplements(@Param("id") UUID id);
}
