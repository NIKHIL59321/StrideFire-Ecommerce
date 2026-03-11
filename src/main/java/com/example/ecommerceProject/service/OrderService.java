package com.example.ecommerceProject.service;

import com.example.ecommerceProject.model.*;
import com.example.ecommerceProject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    private static final List<String> VALID_STATUSES = List.of(
            "PLACED", "SHIPPED", "DELIVERED", "CANCELLED"
    );

    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
            "PLACED",    List.of("SHIPPED", "CANCELLED"),
            "SHIPPED",   List.of("DELIVERED"),
            "DELIVERED", List.of(),
            "CANCELLED", List.of()
    );


    @Transactional
    public Map<String, Object> placeOrder(Long userId) {
        Map<String, Object> response = new HashMap<>();

        // Step 1 — Get all cart items for this user
        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        // Step 2 — Check if cart is empty
        if (cartItems.isEmpty()) {
            response.put("error", "Cart is empty. Please add products before placing order");
            return response;
        }

        // Step 3 — Validate stock for each product
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found with id: " + cartItem.getProductId()));

            if (product.getStocks() < cartItem.getQuantity()) {
                response.put("error", "Insufficient stock for product: " + product.getName()
                        + ". Available: " + product.getStocks());
                return response;
            }
        }

        // Step 4 — Calculate total price
        double total = 0;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(
                    cartItem.getProductId()).get();
            total += product.getPrice() * cartItem.getQuantity();
        }

        // Step 5 — Create and save order
        Order order = new Order();
        order.setUserId(userId);
        order.setTotal(total);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // Step 6 — Create and save order items + reduce stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(
                    cartItem.getProductId()).get();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            // ✅ Save order item to DB
            orderItemRepository.save(orderItem);
            orderItems.add(orderItem);

            // ✅ Reduce stock
            product.setStocks(product.getStocks() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Step 7 — Clear cart
        cartService.clearCart(userId);

        // Step 8 — Return response
        response.put("message", "Order placed successfully");
        response.put("orderId", savedOrder.getId());
        response.put("total", total);
        response.put("status", savedOrder.getStatus());
        response.put("createdAt", savedOrder.getCreatedAt());
        response.put("orderItems", orderItems);

        return response;
    }


    public Map<String, Object> getOrders(Long userId, Long loggedInUserId, String status) {
        Map<String, Object> response = new HashMap<>();

        // Step 1 — Check if user exists
        if (!userRepository.existsById(userId)) {
            response.put("error", "User not found with id: " + userId);
            return response;
        }

        // Step 2 — Check if logged in user matches requested user
        if (!userId.equals(loggedInUserId)) {
            response.put("error", "Access Denied. You can only view your own orders");
            return response;
        }

        // Step 3 — Validate status filter if provided
        if (status != null && !VALID_STATUSES.contains(status.toUpperCase())) {
            response.put("error", "Invalid status filter. Valid values are: " + VALID_STATUSES);
            return response;
        }

        // Step 4 — Fetch orders based on status filter
        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByUserIdAndStatus(userId, status.toUpperCase());
        } else {
            orders = orderRepository.findByUserId(userId);
        }

        // Step 5 — Check if any orders exist
        if (orders.isEmpty()) {
            response.put("message", "No orders found for this user");
            response.put("orders", new ArrayList<>());
            return response;
        }

        // Step 6 — Build order list with order items
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getId());
            orderMap.put("total", order.getTotal());
            orderMap.put("status", order.getStatus());
            orderMap.put("createdAt", order.getCreatedAt());

            Optional<Payment> optionalPayment = paymentRepository.findByOrderId(order.getId());
            if(optionalPayment.isPresent()){
                Payment payment = optionalPayment.get();
                orderMap.put("paymentStatus", payment.getStatus());
                orderMap.put("paymentMethod", payment.getPaymentMethod());
                orderMap.put("transactionId", payment.getTransactionId());
                System.out.println(payment.getStatus()+"------------------------------------------");
            }else{
                orderMap.put("paymentStatus", "PENDING");
                orderMap.put("paymentMethod", "N/A");
                orderMap.put("transactionId", "N/A");
            }


            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            if(orderItems == null || orderItems.isEmpty()){
                orderMap.put("orderItems", new ArrayList<>());
            } else {
                List<Map<String, Object>> enrichedItems = new ArrayList<>();
                for (OrderItem  item : orderItems) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("productId", item.getProductId());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("price", item.getPrice());

                    Optional<Product> optionalProduct = productRepository.findById(item.getProductId());

                    if(optionalProduct.isPresent()){
                        Product product = optionalProduct.get();
                        itemMap.put("productName", product.getName());
                        itemMap.put("imageUrl", product.getImageUrl());
                        itemMap.put("size", product.getSize());
                        itemMap.put("color", product.getColor());
                        itemMap.put("brand", product.getBrand());
                        itemMap.put("category", product.getCategory());
                    }


                    enrichedItems.add(itemMap);

                }
                orderMap.put("orderItems", enrichedItems);
            }
            orderList.add(orderMap);
        }

        response.put("message", "Orders fetched successfully");
        response.put("totalOrders", orderList.size());
        response.put("orders", orderList);

        return response;
    }

    @Transactional
    public Map<String, Object> updateStatus(Long orderId, Long userId, String newStatus) {
        Map<String, Object> response = new HashMap<>();

        // Step 1 — Check if user exists
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            response.put("error", "User not found with id: " + userId);
            return response;
        }

        // Step 2 — Check if user is Admin
        if (!optionalUser.get().getRole().equals("ADMIN")) {
            response.put("error", "Access Denied. Only admin can update order status");
            return response;
        }

        // Step 3 — Validate new status
        if (!VALID_STATUSES.contains(newStatus.toUpperCase())) {
            response.put("error", "Invalid status. Allowed values: PLACED, SHIPPED, DELIVERED, CANCELLED");
            return response;
        }

        // Step 4 — Check if order exists
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            response.put("error", "Order not found with id: " + orderId);
            return response;
        }

        Order order = optionalOrder.get();
        String currentStatus = order.getStatus();

        // Step 5 — ✅ Fixed: Check if new status is same as current
        if (currentStatus.equals(newStatus.toUpperCase())) {
            response.put("error", "Order is already in status: " + newStatus.toUpperCase());
            return response;
        }

        // Step 6 — Check if order is in final state
        if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
            response.put("error", "Cannot change status of an order that is already "
                    + currentStatus);
            return response;
        }

        // Step 7 — Check valid status transition
        List<String> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        if (!allowedTransitions.contains(newStatus.toUpperCase())) {
            response.put("error", "Invalid status transition from "
                    + currentStatus + " to " + newStatus.toUpperCase());
            return response;
        }

        // Step 8 — Update and save status
        order.setStatus(newStatus.toUpperCase());
        orderRepository.save(order);

        response.put("message", "Order status updated successfully");
        response.put("orderId", order.getId());
        response.put("previousStatus", currentStatus);
        response.put("newStatus", order.getStatus());

        return response;
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long userId, Long orderId) {
        Map<String, Object> response = new HashMap<>();

        // Step 1 — Check if user exists
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            response.put("error", "User not found with id: " + userId);
            return response;
        }

        User user = optionalUser.get();

        // Step 2 — ✅ Fixed: Check if order exists using isEmpty()
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            response.put("error", "Order not found with id: " + orderId);
            return response;
        }

        Order order = optionalOrder.get();

        // Step 3 — Check role and ownership
        if (user.getRole().equals("ADMIN")) {
            System.out.println("Admin cancelling order: " + orderId);
        } else if (user.getRole().equals("USER")) {
            if (!order.getUserId().equals(userId)) {
                response.put("error", "Access Denied. You can only cancel your own orders");
                return response;
            }
        } else {
            response.put("error", "Invalid user role: " + user.getRole());
            return response;
        }

        // Step 4 — Check order status
        if (order.getStatus().equals("CANCELLED")) {
            response.put("error", "Order is already cancelled");
            return response;
        }

        if (order.getStatus().equals("SHIPPED") || order.getStatus().equals("DELIVERED")) {
            response.put("error", "Cannot cancel an order that is already "
                    + order.getStatus());
            return response;
        }

        // Step 5 — Update status to CANCELLED
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // Step 6 — Restore product stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStocks(product.getStocks() + orderItem.getQuantity());
            productRepository.save(product);
        }

        // Step 7 — Return response
        response.put("message", "Order cancelled successfully");
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());

        return response;
    }

    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with email: " + email));
        return user.getId();
    }
    public void clearCartAfterOrder(Long userId) {
        List<CartItem> items =
                cartRepository.findByUserId(userId);
        cartRepository.deleteAll(items);
        System.out.println(
                "Cart cleared for user: " + userId);
    }
}