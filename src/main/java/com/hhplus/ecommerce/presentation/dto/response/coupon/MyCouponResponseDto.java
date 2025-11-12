package com.hhplus.ecommerce.presentation.dto.response.coupon;

public record MyCouponResponseDto(
    Long userCouponId,
    Long couponId,
    Integer discountAmount,
    String validUntil,
    String status
) {}
