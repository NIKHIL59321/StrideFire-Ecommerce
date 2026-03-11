package com.example.ecommerceProject.service;

import com.example.ecommerceProject.model.User;
import com.example.ecommerceProject.repository.UserRepository;
import com.example.ecommerceProject.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    public Map<String,String> register(User user){
        Map<String,String> response = new HashMap<>();
        //check if email already exists
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            response.put("error","Email already registered");
            return response;
        }
        //Encoding the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        //set default role as USER
        if(user.getRole().isEmpty())
            user.setRole("USER");
        userRepository.save(user);
        response.put("message","User registered successfully");
        return response;
    }
    public Map<String, Object> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if(optionalUser.isEmpty()){
            response.put("error", "User not Found");
            return response;
        }
        User user = optionalUser.get();
        //check password
        if(!passwordEncoder.matches(password, user.getPassword())){
            response.put("error", "Invalid Credentials");
            return response;
        }
        //generate JWT Token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        response.put("token", token);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("message", "Login successful");
        return response;

    }

    public User getUserById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(Long id, User updatedUser){
        User existingUser = getUserById(id);
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        // only update password if provided
        if(updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()){
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    public Map<String, String> deleteUser(Long id){
        Map<String, String> response = new HashMap<>();
        if(!userRepository.existsById(id)){
            response.put("Error", "User not found");
            return response;
        }
        userRepository.deleteById(id);
        response.put("message", "User deleted successfully");
        return response;
    }
    
    // change password
    public Map<String, String> changePasswordByEmail(
            String email, String oldPassword, String newPassword) {

        Map<String, String> response = new HashMap<>();

        // Step 1 — Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 2 — Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            response.put("error", "Old password is incorrect");
            return response;
        }

        // Step 3 — Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        response.put("message", "Password changed successfully");
        return response;
    }
    // ─────────────────────────────────────────
// FIND OR CREATE GOOGLE USER
// ─────────────────────────────────────────
    public User findOrCreateGoogleUser(
            String email, String name) {

        // Check if user already exists
        Optional<User> existingUser =
                userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // User exists → return existing user
            System.out.println("Existing Google user: " + email);
            return existingUser.get();
        }

        // User not exists → create new user
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword("GOOGLE_AUTH");  // No password for OAuth users
        newUser.setRole("USER");

        User savedUser = userRepository.save(newUser);
        System.out.println("New Google user created: " + email);

        return savedUser;
    }

    // ─────────────────────────────────────────
// CHECK IF USER IS GOOGLE USER
// ─────────────────────────────────────────
    public boolean isGoogleUser(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent() &&
                user.get().getPassword()
                        .equals("GOOGLE_AUTH");
    }
}
