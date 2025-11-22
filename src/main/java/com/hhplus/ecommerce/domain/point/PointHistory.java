package com.hhplus.ecommerce.domain.point;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포인트 이력 엔티티
 * - 포인트 적립/사용 내역 추적
 * - 주문 취소 시 복구 추적용
 */
@Entity
@Table(
    name = "point_histories",
    indexes = @Index(name = "idx_user_created", columnList = "user_id, created_at")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;  // +적립, -사용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;

    @Column(length = 100)
    private String reason;  // "주문 적립", "주문 사용", "주문 취소 복구" 등

    @Column(name = "order_id")
    private Long orderId;  // 주문 연관 추적

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PointHistory(Long userId, Integer changeAmount, PointType type, String reason) {
        this.userId = userId;
        this.changeAmount = changeAmount;
        this.type = type;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public PointHistory(Long userId, Integer changeAmount, PointType type, String reason, Long orderId) {
        this.userId = userId;
        this.changeAmount = changeAmount;
        this.type = type;
        this.reason = reason;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
