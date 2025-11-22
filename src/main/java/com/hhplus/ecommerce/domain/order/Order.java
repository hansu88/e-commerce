package com.hhplus.ecommerce.domain.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 주문 엔티티 (캡슐화 개선)
 * - Setter 제거, Builder 패턴 적용
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_status_created", columnList = "status, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 유저 ,관리자용
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;        // 최종 결제 금액 (할인 적용 후)

    @Column(name = "discount_amount")
    private Integer discountAmount;     // 할인 금액 (쿠폰 할인)

    @Column(name = "user_coupon_id")
    private Long userCouponId;          // 사용한 쿠폰 ID (취소 시 복구용)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 주문 결제
     */
    public void pay() {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제된 주문입니다.");
        }
        this.status = OrderStatus.PAID;
    }
}