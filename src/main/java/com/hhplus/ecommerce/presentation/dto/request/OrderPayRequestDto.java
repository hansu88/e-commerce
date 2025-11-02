package com.hhplus.ecommerce.presentation.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderPayRequestDto {
    private String status;
    private String paymentMethod;
}