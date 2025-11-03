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
    private Integer changeQuantity;
    private StockChangeReason reason;
    private LocalDateTime createdAt;
}