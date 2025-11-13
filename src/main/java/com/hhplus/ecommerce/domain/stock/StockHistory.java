package com.hhplus.ecommerce.domain.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 이력 Entity
 */
@Entity
@Table(name = "stock_histories")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockChangeReason reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StockHistory(Long productOptionId, Integer changeQty, StockChangeReason reason) {
        this.productOptionId = productOptionId;
        this.changeQty = changeQty;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

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