package com.hhplus.ecommerce.presentation.dto.response.cart;

public record CartItemResponseDto(
    Long cartItemId,
    ProductOption productOption,
    Integer quantity
) {
    public record ProductOption(
        Long id,
        String color,
        String size
    ) {}
}