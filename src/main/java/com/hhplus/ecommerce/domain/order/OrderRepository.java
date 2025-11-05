package com.hhplus.ecommerce.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * Order Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface OrderRepository {
    /**
     * 주문 저장
     */
    Order save(Order order);

    /**
     * ID로 주문 조회
     */
    Optional<Order> findById(Long id);

    /**
     * 모든 주문 조회
     */
    List<Order> findAll();
}
