package com.example.ecommerceProject.repository;

import com.example.ecommerceProject.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Get all items belonging to a specific order
    List<OrderItem> findByOrderId(Long orderId);
}
