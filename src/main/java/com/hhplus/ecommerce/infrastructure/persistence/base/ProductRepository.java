package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품 Repository 인터페이스
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
