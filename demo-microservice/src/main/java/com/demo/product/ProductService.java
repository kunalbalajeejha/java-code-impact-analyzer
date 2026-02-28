package com.demo.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAvailableProducts() {
        return productRepo.findByStockGreaterThan(0);
    }

    @Transactional
    public void reserveStock(Long productId, int qty) {
        log.info("Reserving {} units of product {}", qty, productId);
        int updated = productRepo.decrementStock(productId, qty);
        if (updated == 0) {
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }
    }
}
