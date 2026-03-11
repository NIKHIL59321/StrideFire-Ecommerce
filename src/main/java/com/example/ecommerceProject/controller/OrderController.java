package com.example.ecommerceProject.controller;

import com.example.ecommerceProject.service.OrderService;
import com.example.ecommerceProject.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtUtil jwtUtil;

    // helper extract role from JWT token
    private String getRoleFromToken(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.extractRole(token);
    }
    private String getEmailFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Validate userId is not null
        Step 3 → Call orderService.placeOrder(userId)
        Step 4 → If error → return 400 BAD REQUEST
        Step 5 → Return order details with 201 CREATED
     */
    @PostMapping("/place")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader){
        Map<String,Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // Step 2 → Validate userId is not null
        if(request.get("userId")==null){
            response.put("error", "User ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Long userId = Long.valueOf((Integer) request.get("userId"));
        response = orderService.placeOrder(userId);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Extract email from JWT
        Step 3 → status filter is optional
                   └── If provided pass it to service
                   └── If not provided fetch all orders
        Step 4 → Call orderService.getOrders(userId, loggedInUserId, status)
        Step 5 → If error → return proper status code
        Step 6 → Return orders with 200 OK
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String,Object>> getOrders(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status) {
        Map<String,Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // Step 2 → Extract email from JWT
        String email = getEmailFromToken(authHeader);
        Long loggedInUserId = orderService.getUserIdByEmail(email);

        response = orderService.getOrders(userId, loggedInUserId, status);

        if(response.containsKey("error")){
            String error = (String) response.get("error");
            if (error.contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else if (error.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
        return ResponseEntity.ok(response);
    }
    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Extract role from JWT
        Step 3 → Check if role is ADMIN
                   └── If not admin → return 403 FORBIDDEN
        Step 4 → Validate new status is not null
        Step 5 → Call orderService.updateStatus(orderId, userId, newStatus)
        Step 6 → If error → return proper status code
        Step 7 → Return updated order with 200 OK
     */
    @PutMapping("/status/{orderId}")
    public ResponseEntity<Map<String,Object>> updateStatus(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        Map<String,Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        String role = getRoleFromToken(authHeader);
        // Step 3 → Check if role is ADMIN
        if(!role.equals("ADMIN")){
            response.put("error", "Access denied: Admin role required");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        // Step 4 → Validate new status is not null
        if(request.get("status")==null){
            response.put("error", "New status is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        String email = getEmailFromToken(authHeader);
        Long userId = orderService.getUserIdByEmail(email);
        String newstatus = (String) request.get("status");
        response = orderService.updateStatus(orderId, userId, newstatus);
        if (response.containsKey("error")){
            String error = (String) response.get("error");
            if (error.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
        return ResponseEntity.ok(response);
    }
    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Validate orderId is not null
        Step 3 → Validate userId is not null
        Step 4 → Call orderService.cancelOrder(orderId, userId)
        Step 5 → If error → return proper status code
        Step 6 → Return success message with 200 OK
     */

    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // Step 2 → Validate orderId is not null
        if (orderId == null) {
            response.put("error", "Order ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // Step 3 → Validate userId is not null
        if (request.get("userId") == null) {
            response.put("error", "User ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Long userId = Long.valueOf((Integer) request.get("userId"));
        System.out.println("userID-------------------------------------------------"+userId);
        response = orderService.cancelOrder(userId, orderId);
        if (response.containsKey("error")) {
            String error = (String) response.get("error");
            if (error.contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else if (error.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
        return ResponseEntity.ok(response);
    }


}
