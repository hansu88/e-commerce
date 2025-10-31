package com.hhplus.ecommerce.cart.dto;

public class CartItemResponseDto {
    private Long cartItemId;
    private ProductOption productOption;
    private Integer quantity;

    public static class ProductOption {
        private Long id;
        private String color;
        private String size;
    }
}