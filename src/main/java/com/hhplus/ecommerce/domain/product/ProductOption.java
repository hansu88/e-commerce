package com.hhplus.ecommerce.domain.product;

import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 상품 옵션 Entity (캡슐화 개선)
 * - Setter 제거, Builder 패턴 적용
 * - 비즈니스 로직 메서드로 캡슐화
 */
@Entity
@Table(
        name = "product_options",
        indexes = @Index(name = "idx_product_id", columnList = "product_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String color;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(nullable = false)
    private Integer stock;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 차감
     * @param quantity 차감할 수량
     * @throws IllegalArgumentException 수량이 0 이하인 경우
     * @throws OutOfStockException 재고가 부족한 경우
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 양수여야 합니다");
        }
        if (this.stock < quantity) {
            throw new OutOfStockException("재고가 부족합니다. 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }
        this.stock -= quantity;
    }

    /**
     * 재고 증가
     * @param quantity 증가할 수량
     * @throws IllegalArgumentException 수량이 0 이하인 경우
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("증가 수량은 양수여야 합니다");
        }
        this.stock += quantity;
    }
}