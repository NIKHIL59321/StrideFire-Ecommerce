package com.example.ecommerceProject.repository;

import com.example.ecommerceProject.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Used in → getOrders()
    // Get all orders for a specific user
    List<Order> findByUserId(Long userId);

    // Used in → updateStatus()
    // Get orders by status (PLACED, SHIPPED, DELIVERED, CANCELLED)
    List<Order> findByStatus(String status);

    // Used in → getOrders()
    // Get all orders for a user by status
    List<Order> findByUserIdAndStatus(Long userId, String status);

}
