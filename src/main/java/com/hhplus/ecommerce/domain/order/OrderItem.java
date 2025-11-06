package com.hhplus.ecommerce.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 항목 엔티티
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productOptionId;
    private Integer quantity;
    private Integer price;
    private LocalDateTime createdAt;
}
