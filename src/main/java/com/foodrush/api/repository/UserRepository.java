package com.foodrush.api.repository;

import com.foodrush.api.enums.RoleType;
import com.foodrush.api.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  @Override
  @EntityGraph(attributePaths = { "address" })
  List<User> findAll();

  @Override
  @EntityGraph(attributePaths = { "address" })
  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByPhone(String phone);

  Optional<User> findByCpf(String cpf);

  boolean existsByCpf(String cpf);

  boolean existsByPhone(String phone);

  long countByRole(RoleType role);

  long countByActive(Boolean active);
}
