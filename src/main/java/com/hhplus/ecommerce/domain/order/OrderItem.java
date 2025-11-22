package com.hhplus.ecommerce.domain.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 주문 항목 엔티티 (캡슐화 개선)
 * - 인기 상품 집계를 위해 createdAt 필드 포함
 * - Setter 제거, Builder 패턴 적용
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),  // 집계 쿼리 최적화
        @Index(name = "idx_created_product",  // 인기 상품 집계 쿼리 최적화
               columnList = "created_at, product_option_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 유저 ,관리자용
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

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
