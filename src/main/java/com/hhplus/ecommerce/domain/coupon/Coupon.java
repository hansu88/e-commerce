package com.hhplus.ecommerce.domain.coupon;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 쿠폰 Entity
 */
@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_code", columnList = "code", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
