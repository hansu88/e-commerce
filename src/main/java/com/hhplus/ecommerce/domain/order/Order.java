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
    private Integer totalAmount;        // 최종 결제 금액 (할인 적용 후)
    private Integer discountAmount;     // 할인 금액 (쿠폰 할인)
    private Long userCouponId;          // 사용한 쿠폰 ID (취소 시 복구용)
    private LocalDateTime createdAt;
}
