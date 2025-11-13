package com.hhplus.ecommerce.domain.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 엔티티
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}