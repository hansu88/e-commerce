package com.hhplus.ecommerce.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 항목 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productOptionId;
    private Integer quantity;
    private Integer price;  // 주문 당시 가격 (나중에 상품 가격이 변해도 주문 내역은 유지)

}