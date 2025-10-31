package com.hhplus.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderPayResponseDto {
    private Long orderId;
    private String status;
}
