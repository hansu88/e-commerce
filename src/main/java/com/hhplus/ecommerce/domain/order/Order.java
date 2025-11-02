package com.hhplus.ecommerce.domain.order;

import java.time.LocalDateTime;

/**
 * 주문 Entity
 */
public class Order {
    private Long id;
    private Long userId;
    private OrderStatus status;
    private Integer totalAmount;
    private LocalDateTime createdAt;

    // 생성자
    public Order(Long id, Long userId, OrderStatus status, Integer totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    // Getter
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public Integer getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter
    public void setId(Long id) { this.id = id; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
}