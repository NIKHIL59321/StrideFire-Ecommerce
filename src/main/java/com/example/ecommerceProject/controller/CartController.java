package com.example.ecommerceProject.controller;

import com.example.ecommerceProject.model.User;
import com.example.ecommerceProject.service.CartService;
import com.example.ecommerceProject.service.UserService;
import com.example.ecommerceProject.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from the React frontend
public class CartController {
    @Autowired
    private CartService cartService;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    private String getEmailFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    // add to cart
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        // validate user
        if(request.get("userId")==null){
            response.put("error", "User ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // validate product
        if(request.get("productId")==null){
            response.put("error", "Product ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // validate quantity
        if(request.get("quantity")==null){
            response.put("error", "Quantity is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        int quantity = (int) request.get("quantity");
        if(quantity <= 0){
            response.put("error", "Quantity must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Long usesId = Long.valueOf((Integer) request.get("userId"));
        Long productId = Long.valueOf((Integer) request.get("productId"));

        // call service
        response = cartService.addToCart(usesId, productId, quantity);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Extract email from JWT
        Step 3 → Check if logged in user matches userId
                   └── User cannot see another user's cart
        Step 4 → Call cartService.getCart(userId)
        Step 5 → If empty → return "Cart is empty"
        Step 6 → Return cart items with total price
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getCart(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        // Step 1 & 2
        String email = getEmailFromToken(authHeader);
        User user = userService.getUserByEmail(email);
        if(!user.getId().equals(userId)){
            response.put("error", "Unauthorized: You can only access your own cart");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        response = cartService.getCart(userId);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Validate quantity
                   ├── Must be greater than 0
                   └── If quantity is 0 → suggest removeItem instead
        Step 3 → Call cartService.updateQuantity(cartItemId, quantity)
        Step 4 → If not found → return 404
        Step 5 → Return updated cart item with 200 OK
     */

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<Map<String, Object>> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // Step 2 → Validate quantity
        int quantity = (int) request.get("quantity");
        if(quantity<=0){
            response.put("error", "Quantity must be greater than 0. To remove item, use the remove endpoint.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // Step 3 → Call cartService.updateQuantity(cartItemId, quantity)
        response = cartService.updateQuantity(cartItemId, quantity);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /*
        Step 1 → Check if user is logged in (JWT required)
        Step 2 → Check if cart item exists
                   └── If not found → return 404
        Step 3 → Call cartService.removeItem(cartItemId)
        Step 4 → Return success message with 200 OK
     */
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Map<String,Object>> removeItem(
            @PathVariable Long cartItemId,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        // Step 1 → Check if user is logged in (JWT required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Unauthorized: JWT token is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        response = cartService.removeItem(cartItemId);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<Map<String, String>> clearCart(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {

        // Call service
        Map<String, String> serviceResponse = cartService.clearCart(userId);

        if (serviceResponse.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResponse);
        }

        return ResponseEntity.ok(serviceResponse);
    }
}
