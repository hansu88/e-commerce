package com.hhplus.ecommerce.domain.cart;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 장바구니 Entity (캡슐화 개선)
 * - Setter 제거, Builder 패턴 적용
 */
@Entity
@Table(
    name = "carts",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id", unique = true)
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// 관리자용
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

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
