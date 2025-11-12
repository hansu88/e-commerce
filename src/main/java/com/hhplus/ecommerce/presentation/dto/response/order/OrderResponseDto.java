package com.hhplus.ecommerce.presentation.dto.response.order;

public record OrderResponseDto(
        Long orderId,
        String status,
        Long appliedCouponId
) {}