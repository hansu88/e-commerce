package com.hhplus.ecommerce.domain.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 Entity (발급된 쿠폰)
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;       // 사용 일시 (nullable)
    private UserCouponStatus status;

}
