package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.stock.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * StockHistory Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByProductOptionId(Long productOptionId);
}
