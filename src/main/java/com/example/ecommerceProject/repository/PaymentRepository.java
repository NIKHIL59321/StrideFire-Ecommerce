package com.example.ecommerceProject.repository;

import com.example.ecommerceProject.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    Optional<Payment> findByTransactionId(String transactionId);
}
