package com.hhplus.ecommerce.domain.stock;

import java.time.LocalDateTime;

/**
 * 재고 이력 Entity
 */
public class StockHistory {
    private Long id;
    private Long productOptionId;
    private Integer changeQuantity;
    private StockChangeReason reason;
    private LocalDateTime createdAt;

    // 생성자
    public StockHistory(Long id, Long productOptionId, Integer changeQuantity,
                        StockChangeReason reason, LocalDateTime createdAt) {
        this.id = id;
        this.productOptionId = productOptionId;
        this.changeQuantity = changeQuantity;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    // Getter
    public Long getId() { return id; }
    public Long getProductOptionId() { return productOptionId; }
    public Integer getChangeQuantity() { return changeQuantity; }
    public StockChangeReason getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter
    public void setId(Long id) { this.id = id; }
}