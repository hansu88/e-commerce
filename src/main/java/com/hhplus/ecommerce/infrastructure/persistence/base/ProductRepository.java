package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.product.Product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository 인터페이스
 */
public interface ProductRepository {

    /**
     * 상품 저장 (데이터 저장)
     */
    Product save(Product product);

    /**
     * ID로 상품 조회 (GET /api/products/{id})
     */
    Optional<Product> findById(Long id);

    /**
     * 모든 상품 조회 (GET /api/products)
     */
    List<Product> findAll();
}
