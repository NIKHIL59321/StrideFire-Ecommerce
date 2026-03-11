package com.example.ecommerceProject.service;

import com.example.ecommerceProject.model.CartItem;
import com.example.ecommerceProject.model.Product;
import com.example.ecommerceProject.repository.CartRepository;
import com.example.ecommerceProject.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    public Map<String, Object> addToCart(Long userId, Long productId, int quantity) {
        Map<String, Object> response = new HashMap<>();

        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isEmpty()) {
            response.put("error", "Product not found");
            return response;
        }

        Product product = optionalProduct.get();

        if (product.getStocks() <= 0) {
            response.put("error", "Product out of stock");
            return response;
        }

        if (product.getStocks() < quantity) {
            response.put("error", "Only " + product.getStocks() + " items available");
            return response;
        }

        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductId(userId, productId);
        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;

            if (newQuantity > product.getStocks()) {
                response.put("error", "Only " + product.getStocks() + " items available");
                return response;
            }

            cartItem.setQuantity(newQuantity);
            cartRepository.save(cartItem);

            response.put("message", "Cart updated successfully");
            response.put("cartItem", cartItem);
        } else {
            CartItem newCartItem = new CartItem();
            newCartItem.setQuantity(quantity);
            newCartItem.setProductId(productId);
            newCartItem.setUserId(userId);
            cartRepository.save(newCartItem);

            response.put("message", "Product added to cart successfully");
            response.put("cartItem", newCartItem);
        }

        return response;
    }


    public Map<String, Object> getCart(Long userId) {
        Map<String, Object> response = new HashMap<>();

        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            response.put("message", "Cart is empty");
            response.put("cartItems", new ArrayList<>());
            response.put("total", 0.0);
            return response;
        }

        List<Map<String, Object>> cartItemDetails = new ArrayList<>();



        // Calculate total
        double total = 0;
        int totalItems = 0;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElse(null);
            if (product != null) {
                Map<String, Object> itemDetail = new HashMap<>();

                itemDetail.put("id",cartItem.getId());
                itemDetail.put("productId", cartItem.getProductId());
                itemDetail.put("quantity", cartItem.getQuantity());

                itemDetail.put("productName", product.getName());
                itemDetail.put("imageUrl", product.getImageUrl());
                itemDetail.put("price", product.getPrice());
                itemDetail.put("size", product.getSize());
                itemDetail.put("color", product.getColor());
                itemDetail.put("brand", product.getBrand());

                cartItemDetails.add(itemDetail);

                total+= product.getPrice()*cartItem.getQuantity();

                totalItems+= cartItem.getQuantity();
            }
        }

        response.put("message", "Cart fetched successfully");
        response.put("cartItems", cartItemDetails);
        response.put("totalItems", totalItems);
        response.put("total", total);
        return response;
    }


    public Map<String, Object> removeItem(Long cartItemId) {
        Map<String, Object> response = new HashMap<>();

        if (cartRepository.existsById(cartItemId)) {
            cartRepository.deleteById(cartItemId);
            response.put("message", "Item removed from cart successfully");
        } else {
            response.put("error", "Cart item not found");
        }

        return response;
    }

    public Map<String, Object> updateQuantity(Long cartItemId, int quantity) {
        Map<String, Object> response = new HashMap<>();

        Optional<CartItem> optionalCartItem = cartRepository.findById(cartItemId);
        if (optionalCartItem.isEmpty()) {
            response.put("error", "Cart item not found");
            return response;
        }

        CartItem cartItem = optionalCartItem.get();

        Optional<Product> optionalProduct = productRepository.findById(cartItem.getProductId());
        if (optionalProduct.isEmpty()) {
            response.put("error", "Product not found");
            return response;
        }

        Product product = optionalProduct.get();

        if (quantity > product.getStocks()) {
            response.put("error", "Only " + product.getStocks() + " items available");
            return response;
        }

        cartItem.setQuantity(quantity);
        cartRepository.save(cartItem);

        response.put("message", "Cart item quantity updated successfully");
        response.put("cartItem", cartItem);
        return response;
    }


    @Transactional
    public Map<String, String> clearCart(Long userId) {
        Map<String, String> response = new HashMap<>();

        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            response.put("error", "Cart is already empty");
            return response;
        }

        cartRepository.deleteAllByUserId(userId);
        response.put("message", "Cart cleared successfully");
        return response;
    }
}