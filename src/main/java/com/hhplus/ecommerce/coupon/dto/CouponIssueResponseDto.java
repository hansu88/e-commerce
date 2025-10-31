package com.hhplus.ecommerce.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponIssueResponseDto {
    private Long couponId;
    private String issuedAt;
}
