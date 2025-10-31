package com.hhplus.ecommerce.order.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderPayRequestDto {
    private String status;
    private String paymentMethod;
}