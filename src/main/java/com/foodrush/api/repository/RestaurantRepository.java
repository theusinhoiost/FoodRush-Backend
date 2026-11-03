package com.foodrush.api.repository;

import com.foodrush.api.model.Restaurant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
  @Override
  @EntityGraph(attributePaths = { "owner", "address" })
  List<Restaurant> findAll();

  @Override
  @EntityGraph(attributePaths = { "owner", "address" })
  Optional<Restaurant> findById(UUID id);

  Optional<Restaurant> findByCnpj(String cnpj);

  boolean existsByCnpj(String cnpj);

  boolean existsByPhone(String phone);

  List<Restaurant> findAllByIsOpenTrue();

  List<Restaurant> findAllByOwnerId(UUID ownerId);

  long countByIsOpen(Boolean isOpen);
}
