package com.hhplus.ecommerce.domain.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 Entity
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private Integer price;
    private ProductStatus status;
    private LocalDateTime createdAt;

    private List<ProductOption> options;

    public Product(String name, Integer price, ProductStatus status) {
        this.name = name;
        this.price = price;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}