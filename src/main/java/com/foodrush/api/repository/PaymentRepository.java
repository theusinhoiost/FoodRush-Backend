package com.foodrush.api.repository;

import com.foodrush.api.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByExternalId(String externalId);

  List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  @Query("SELECT p FROM Payment p JOIN FETCH p.order o JOIN FETCH o.customer WHERE p.externalId = :externalId")
  Optional<Payment> findByExternalIdWithOrder(@Param("externalId") String externalId);

  Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
