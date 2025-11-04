package com.hhplus.ecommerce.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 엔티티
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private OrderStatus status;
    private Integer totalAmount;
    private LocalDateTime createdAt;
}