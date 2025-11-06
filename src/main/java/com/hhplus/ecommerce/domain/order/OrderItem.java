package com.hhplus.ecommerce.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 항목 엔티티
 * 인기 상품 집계를 위해 createdAt 필드 포함
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
    

    /**
     * 인기 상품 집계를 위한 헬퍼 메서드
     * ProductOption의 productId를 얻기 위해 사용
     */
    public Long getProductId() {
        // TODO: ProductOption에서 productId 조회 필요
        // 현재는 별도 메서드로 처리
        return null;
    }
}
