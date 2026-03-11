package com.example.ecommerceProject.repository;

import com.example.ecommerceProject.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId); // used for get cart
    void deleteById(Long id); // used for remove item from cart

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId); // Used in → addToCart() — check if product already in cart

    void deleteAllByUserId(Long userId);// Used in → clearCart() after order placed
}
