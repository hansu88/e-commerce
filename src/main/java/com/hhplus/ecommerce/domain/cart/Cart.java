package com.hhplus.ecommerce.domain.cart;

import java.time.LocalDateTime;

/**
 * 장바구니 Entity
 */
public class Cart {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;

    // 생성자
    public Cart(Long id, Long userId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Getter
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter
    public void setId(Long id) { this.id = id; }
}
