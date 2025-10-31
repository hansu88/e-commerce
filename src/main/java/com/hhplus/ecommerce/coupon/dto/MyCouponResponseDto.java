package com.hhplus.ecommerce.coupon.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyCouponResponseDto {
    private Long userCouponId;
    private Long couponId;
    private Integer discountAmount;
    private String validUntil;
    private String status;
}
