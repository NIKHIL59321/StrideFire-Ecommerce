package com.example.ecommerceProject.service;

import com.example.ecommerceProject.model.Product;
import com.example.ecommerceProject.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public Product addProduct(Product product) {
        if(productRepository.existsByName(product.getName())){
            throw new RuntimeException("Product with this name already exists");
        }
        return productRepository.save(product);
    }
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    public Product getById(Long id){
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
    public void deleteProduct(Long id) {
        if(!productRepository.existsById(id)){
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id).map(product->{
            product.setBrand(updatedProduct.getBrand());
            product.setName(updatedProduct.getName());
            product.setCategory(updatedProduct.getCategory());
            product.setColor(updatedProduct.getColor());
            product.setPrice(updatedProduct.getPrice());
            product.setSize(updatedProduct.getSize());
            product.setStocks(updatedProduct.getStocks());
            return productRepository.save(product);
        })
        .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
}
