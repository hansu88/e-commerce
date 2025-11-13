package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderItem Repository 인터페이스
 * Domain 계층에 위치 (DIP)
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 특정 기간 내 생성된 OrderItem 조회
     * - 집계 로직에서 사용
     * - findAll() + 메모리 필터링 대신 DB 쿼리로 처리
     */
    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.createdAt >= :startDateTime " +
            "AND oi.createdAt < :endDateTime")
    List<OrderItem> findByCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
