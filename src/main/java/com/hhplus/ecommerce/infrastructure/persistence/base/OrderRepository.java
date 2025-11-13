package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Order Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}
