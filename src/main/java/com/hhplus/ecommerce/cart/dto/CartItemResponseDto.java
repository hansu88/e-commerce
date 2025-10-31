package com.hhplus.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponseDto {
    private Long cartItemId;
    private ProductOption productOption;
    private Integer quantity;

    @Getter
    @AllArgsConstructor
    public static class ProductOption {
        private Long id;
        private String color;
        private String size;
    }
}