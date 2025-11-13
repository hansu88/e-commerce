package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.product.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 상품 옵션 Repository 인터페이스
 */
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    List<ProductOption> findByProductId(Long productId);
}
