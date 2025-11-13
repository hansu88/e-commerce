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
     * - Native Query로 Product와 ProductOption을 JOIN하여 재고 합계 계산
     * - 1번의 쿼리로 모든 데이터 조회
     * - FK 없이 product_id 컬럼으로 JOIN
     */
    @Query(value = "SELECT p.id, p.name, p.price, p.status, " +
            "COALESCE(SUM(po.stock), 0) as total_stock " +
            "FROM products p " +
            "LEFT JOIN product_options po ON p.id = po.product_id " +
            "GROUP BY p.id, p.name, p.price, p.status " +
            "ORDER BY p.id",
            nativeQuery = true)
    List<Object[]> findAllWithTotalStockNative();
}
