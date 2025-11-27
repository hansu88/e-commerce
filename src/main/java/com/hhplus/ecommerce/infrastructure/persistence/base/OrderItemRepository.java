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

    /**
     * 상품별 판매량 집계 (Native SQL 최적화)
     * - JOIN으로 N+1 문제 해결
     * - DB에서 GROUP BY 집계 (메모리 집계 제거)
     * - idx_created_product 복합 인덱스 활용
     *
     * @return [product_id, total_quantity]
     */
    @Query(value =
            "SELECT po.product_id, SUM(oi.quantity) as total_quantity " +
            "FROM order_items oi " +
            "INNER JOIN product_options po ON oi.product_option_id = po.id " +
            "WHERE oi.created_at >= :startDateTime " +
            "  AND oi.created_at < :endDateTime " +
            "GROUP BY po.product_id " +
            "ORDER BY total_quantity DESC",
            nativeQuery = true)
    List<Object[]> aggregateProductSalesByPeriod(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
