package com.hhplus.ecommerce.domain.product;

import java.time.LocalDateTime;

/**
 * 상품 Entity
 */
public class Product {
    private Long id;
    private String name;
    private Integer price;
    private ProductStatus status;
    private LocalDateTime createdAt;

    // 생성자
    public Product(Long id, String name, Integer price, ProductStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getter
    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter
    public void setId(Long id) { this.id = id; }
}