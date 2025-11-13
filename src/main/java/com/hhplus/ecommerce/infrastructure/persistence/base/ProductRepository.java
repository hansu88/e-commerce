package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 상품 Repository 인터페이스
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 상품 목록 조회 (DTO 직접 조회로 N+1 문제 해결)
     * - Product와 ProductOption을 JOIN하여 재고 합계 계산
     * - 1번의 쿼리로 모든 데이터 조회
     */
    @Query("SELECT new com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto(" +
            "p.id, " +
            "p.name, " +
            "p.price, " +
            "CAST(p.status AS string), " +
            "COALESCE(SUM(po.stock), 0)) " +
            "FROM Product p " +
            "LEFT JOIN ProductOption po ON po.productId = p.id " +
            "GROUP BY p.id, p.name, p.price, p.status " +
            "ORDER BY p.id")
    List<ProductListResponseDto> findAllWithTotalStock();
}
