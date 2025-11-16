package com.hhplus.ecommerce.domain.coupon;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 Entity (발급된 쿠폰)
 */
@Entity
@Table(
    name = "user_coupons",
    indexes = {
        @Index(name = "idx_user_status", columnList = "user_id, status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_coupon", columnNames = {"user_id", "coupon_id"})  // 중복 발급 방지
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 유저용
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;       // 사용 일시 (nullable)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserCouponStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 만료 처리
     */
    public void expire() {
        this.status = UserCouponStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     *  쿠폰 사용처리
     *  @throws IllegalStateException 이미 사용된 쿠폰인 경
     */
    public void use() {
        if (this.status == UserCouponStatus.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다");
        }
        if (this.status == UserCouponStatus.EXPIRED) {
            throw new IllegalStateException("만료된 쿠폰입니다");
        }

        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 복구 처리 (주문 취소 시)
     */
    public void restore() {
        this.status = UserCouponStatus.AVAILABLE;
        this.usedAt = null;
        this.updatedAt = LocalDateTime.now();
    }
}
