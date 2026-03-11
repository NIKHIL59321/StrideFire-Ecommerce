package com.example.ecommerceProject.controller;

import com.example.ecommerceProject.dto.PaymentRequest;
import com.example.ecommerceProject.service.PaymentService;
import com.example.ecommerceProject.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private JwtUtil jwtUtil;

    // ─────────────────────────────────────────
    // HELPER — Extract email from JWT
    // ─────────────────────────────────────────
    private String getEmailFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    // ─────────────────────────────────────────
    // CREATE PAYMENT INTENT
    // ─────────────────────────────────────────
    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PaymentRequest request) {

        Map<String, Object> response = new HashMap<>();

        // Validate orderId
        if (request.getOrderId() == null) {
            response.put("error", "OrderId is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validate userId
        if (request.getUserId() == null) {
            response.put("error", "UserId is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validate payment method
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            response.put("error", "Payment method is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validate payment method value
        if (!request.getPaymentMethod().equalsIgnoreCase("UPI")
                && !request.getPaymentMethod().equalsIgnoreCase("CARD")) {
            response.put("error", "Invalid payment method. Allowed: UPI, CARD");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validate UPI ID if payment method is UPI
        if (request.getPaymentMethod().equalsIgnoreCase("UPI")) {
            if (request.getUpiId() == null || request.getUpiId().isEmpty()) {
                response.put("error", "UPI ID is required for UPI payment");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            if (!request.getUpiId().contains("@")) {
                response.put("error", "Invalid UPI ID format. Example: username@upi");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // Call service
        Map<String, Object> serviceResponse = paymentService.createPaymentIntent(request);

        if (serviceResponse.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResponse);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(serviceResponse);
    }

    // ─────────────────────────────────────────
    // CONFIRM PAYMENT
    // ─────────────────────────────────────────
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String transactionId) {

        Map<String, Object> response = new HashMap<>();

        // Validate transactionId
        if (transactionId == null || transactionId.isEmpty()) {
            response.put("error", "TransactionId is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Call service
        Map<String, Object> serviceResponse = paymentService.confirmPayment(transactionId);

        if (serviceResponse.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResponse);
        }

        return ResponseEntity.ok(serviceResponse);
    }

    // ─────────────────────────────────────────
    // GET PAYMENT BY ORDER ID
    // ─────────────────────────────────────────
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {

        Map<String, Object> response = new HashMap<>();

        // Validate orderId
        if (orderId == null) {
            response.put("error", "OrderId is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Call service
        Map<String, Object> serviceResponse = paymentService.getPaymentByOrderId(orderId);

        if (serviceResponse.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceResponse);
        }

        return ResponseEntity.ok(serviceResponse);
    }
}