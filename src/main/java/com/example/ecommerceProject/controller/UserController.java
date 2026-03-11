package com.example.ecommerceProject.controller;

import com.example.ecommerceProject.model.User;
import com.example.ecommerceProject.repository.UserRepository;
import com.example.ecommerceProject.service.UserService;
import com.example.ecommerceProject.util.JwtUtil;
import jakarta.websocket.OnClose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    // register
    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, String>> register (@RequestBody User user){
        Map<String, String> response = userService.register(user);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    // login
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest){
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        Map<String, Object> response = userService.login(email,password);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }
    // get user by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id){
        try{
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    //Update User
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,@RequestBody User updatedUser){
        try{
            User user = userService.updateUser(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Delete User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id){
        Map<String, String> response = userService.deleteUser(id);
        if (response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Change Password
    @PutMapping("/users/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> passwordRequest){
        // remove bearer
        String token = authHeader.substring(7);
        // extract email from JWT token
        String email = jwtUtil.extractEmail(token);

        // get old and new password from request
        String oldPassword = passwordRequest.get("oldPassword");
        String newPassword = passwordRequest.get("newPassword");
        //validate new password
        if(newPassword== null|| newPassword.length()<6){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "New password must be at least 6 characters long"));
        }
        if(oldPassword.equals(newPassword)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "New password must be different from old password"));
        }
        // call service to change password
        Map<String, String> response = userService.changePasswordByEmail(email, oldPassword, newPassword);
        if(response.containsKey("error")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }
    // google login
    @GetMapping("/is-google-user")
    public ResponseEntity<Map<String, Object>> isGoogleUser(
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        String email = jwtUtil.extractEmail(
                authHeader.substring(7));

        boolean isGoogleUser = userService.isGoogleUser(email);

        response.put("isGoogleUser", isGoogleUser);
        return ResponseEntity.ok(response);
    }
}
