package com.hhplus.ecommerce.domain.stock;

/**
 * 재고 변경 사유
 */
public enum StockChangeReason {
    ORDER,      // 주문으로 인한 재고 차감
    CANCEL,     // 주문 취소로 인한 재고 증가
    RESTOCK     // 재입고
}
