package com.hhplus.ecommerce.domain.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 쿠폰 Entity (캡슐화 개선)
 * - Setter 제거, Builder 패턴 적용
 * - 비즈니스 로직을 메서드로 캡슐화
 */
@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_code", columnList = "code", unique = true)
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 관리자용
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;      // 총 발급 가능 수량

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity;     // 현재까지 발급된 수량

    @Version
    private Long version;  // 낙관적 락 (동시 발급 제어)

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

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
     * 쿠폰 발급 수량 증가
     * - 비즈니스 규칙: 발급 수량이 총 수량을 초과할 수 없음
     */
    public void increaseIssuedQuantity() {
        if (this.issuedQuantity >= this.totalQuantity) {
            throw new IllegalStateException("쿠폰 발급 한도 초과");
        }
        this.issuedQuantity++;
    }
}
