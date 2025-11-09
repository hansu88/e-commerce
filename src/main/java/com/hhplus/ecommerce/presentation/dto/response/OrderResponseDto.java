package com.hhplus.ecommerce.presentation.dto.response;

public record OrderResponseDto(
    Long orderId,
    String status,
    Long appliedCouponId
) {}