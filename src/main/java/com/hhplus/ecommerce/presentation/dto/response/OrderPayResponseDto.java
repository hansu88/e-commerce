package com.hhplus.ecommerce.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderPayResponseDto {
    private Long orderId;
    private String status;
}
