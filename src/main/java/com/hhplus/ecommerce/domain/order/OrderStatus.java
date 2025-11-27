package com.hhplus.ecommerce.domain.order;

/**
 * 주문 상태
 */
public enum OrderStatus {
    CREATED,    // 주문 생성됨
    PAID,       // 결제 완료
    CANCELLED   // 주문 취소
}
