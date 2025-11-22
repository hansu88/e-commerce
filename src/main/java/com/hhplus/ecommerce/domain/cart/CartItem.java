package com.hhplus.ecommerce.domain.cart;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 장바구니 항목 Entity (캡슐화 개선)
 * - Setter 제거, Builder 패턴 적용
 * - 비즈니스 로직 메서드로 캡슐화
 */
@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_id", columnList = "cart_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cart_product",
            columnNames = {"cart_id", "product_option_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 유저용
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private Integer quantity;

    @Version
    private Long version;  // 낙관적 락 (동시 수량 변경 제어)

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
     * 수량 증가
     * @param additionalQuantity 추가할 수량
     */
    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("추가 수량은 양수여야 합니다");
        }
        this.quantity += additionalQuantity;
    }
}