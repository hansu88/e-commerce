package com.hhplus.ecommerce.domain.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private Integer price;
    private ProductStatus status;
    private LocalDateTime createdAt;

}