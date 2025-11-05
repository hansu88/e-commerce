package com.hhplus.ecommerce.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * OrderItem Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface OrderItemRepository {
    /**
     * 주문 항목 저장
     */
    OrderItem save(OrderItem orderItem);

    /**
     * ID로 주문 항목 조회
     */
    Optional<OrderItem> findById(Long id);

    /**
     * 특정 주문의 모든 항목 조회
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 모든 주문 항목 조회 (인기 상품 집계용)
     */
    List<OrderItem> findAll();
}
