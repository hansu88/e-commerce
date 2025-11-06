package com.hhplus.ecommerce.domain.stock;

import java.util.List;
import java.util.Optional;

/**
 * StockHistory Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface StockHistoryRepository {
    /**
     * 재고 이력 저장
     */
    StockHistory save(StockHistory stockHistory);

    /**
     * ID로 재고 이력 조회
     */
    Optional<StockHistory> findById(Long id);

    /**
     * 특정 상품 옵션의 재고 이력 조회
     */
    List<StockHistory> findByProductOptionId(Long productOptionId);

    /**
     * 모든 재고 이력 조회
     */
    List<StockHistory> findAll();
}
