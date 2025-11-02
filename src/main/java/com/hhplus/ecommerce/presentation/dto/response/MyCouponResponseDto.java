package com.hhplus.ecommerce.presentation.dto.response;


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
