package com.hhplus.ecommerce.domain.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 이력 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class StockHistory {
    private Long id;
    private Long productOptionId;
    private Integer changeQty;
    private StockChangeReason reason;
    private LocalDateTime createdAt;

    public StockHistory(Long productOptionId, Integer changeQty, StockChangeReason reason) {
        this.productOptionId = productOptionId;
        this.changeQty = changeQty;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}