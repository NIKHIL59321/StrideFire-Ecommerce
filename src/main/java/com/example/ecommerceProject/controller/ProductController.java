package com.example.ecommerceProject.controller;

import com.example.ecommerceProject.model.Product;
import com.example.ecommerceProject.service.ProductService;
import com.example.ecommerceProject.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from the React frontend
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private JwtUtil jwtUtil;
    
    // helper extract role from JWT token
    private String getRoleFromToken(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.extractRole(token);
    }
    // get all products public endpoint
    @GetMapping
    public ResponseEntity<?> getAllProducts(){
        List<Product> products = productService.getAllProducts();
        if(products.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No products found"));
        }
        return ResponseEntity.ok(products);
    }
    // get product by id public endpoint
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id){
        try{
            Product product = productService.getById(id);
            return ResponseEntity.ok(product);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    // add product admin endpoint
    /*
        Step 1 → Only ADMIN can add product
                └── Check role from JWT
        Step 2 → Validate product fields
                   ├── Name not empty
                   ├── Price must be greater than 0
                   └── Stock must be greater than 0
        Step 3 → Call productService.addProduct(product)
        Step 4 → Return saved product with 201 CREATED
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> addProduct(
            @RequestBody Product product,
            @RequestHeader("Authorization") String authHeader){
        Map<String, String> response = new HashMap<>();
        //check role
        String role = getRoleFromToken(authHeader);
        if(!"ADMIN".equals(role)){
            response.put("error", "Unauthorized: Only ADMIN can add products");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        //validate fields
        if(product.getName()==null || product.getName().isEmpty()){
            response.put("error", "Product name cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if(product.getPrice()<=0){
            response.put("error", "Price must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if(product.getStocks()<0){
            response.put("error", "Stock must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // validate brand
        if (product.getBrand() == null || product.getBrand().isEmpty()) {
            response.put("error", "Product brand cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if(product.getCategory() == null || product.getCategory().isEmpty()){
            response.put("error", "Product category cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        try {
            Product savedProduct = productService.addProduct(product);
            response.put("message", "Product added successfully");
            response.put("productId", savedProduct.getId().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }catch (Exception e){
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    /*
        Step 1 → Only ADMIN can update product
               └── Check role from JWT
        Step 2 → Check if product exists
                   └── If not found → return 404
        Step 3 → Call productService.updateProduct(id, updatedProduct)
        Step 4 → Return updated product with 200 OK
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String,Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct,
            @RequestHeader("Authorization") String authHeader){
        Map<String, Object> response = new HashMap<>();
        //check role
        String role = getRoleFromToken(authHeader);
        if(!"ADMIN".equals(role)){
            response.put("error", "Unauthorized: Only ADMIN can update products");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        // validate price if provided
        if(updatedProduct.getPrice() <0){
            response.put("error", "Price must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        // validate stock if provided
        if(updatedProduct.getStocks() <0){
            response.put("error", "Stock must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        try{
            Product product = productService.updateProduct(id, updatedProduct);
            response.put("message", "Product updated successfully");
            response.put("product",product);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id){
        Map<String, String> response = new HashMap<>();
        // check role
        String role = getRoleFromToken(authHeader);
        if(!"ADMIN".equals(role)){
            response.put("error", "Unauthorized: Only ADMIN can delete products");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try{
            productService.deleteProduct(id);
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e){
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }




}
