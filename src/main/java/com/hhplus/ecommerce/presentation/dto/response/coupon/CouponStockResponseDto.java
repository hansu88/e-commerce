package com.hhplus.ecommerce.presentation.dto.response.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 쿠폰 재고 응답 DTO (STEP 14)
 */
@Getter
@AllArgsConstructor
public class CouponStockResponseDto {

    private Long couponId;          // 쿠폰 ID
    private Integer totalQuantity;  // 총 수량
    private Integer issuedCount;    // 발급된 수량 (Redis)
    private Integer remainingCount; // 남은 수량
}
