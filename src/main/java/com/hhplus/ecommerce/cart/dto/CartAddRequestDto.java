package com.hhplus.ecommerce.cart.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartAddRequestDto {
    private Long userId;
    private Long productOptionId;
    private Integer quantity;
}