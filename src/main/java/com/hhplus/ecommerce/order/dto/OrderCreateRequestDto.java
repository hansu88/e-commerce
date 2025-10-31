package com.hhplus.ecommerce.order.dto;

import java.util.List;

public class OrderCreateRequestDto {
    private Long userId;
    private List<CartItemInfo> cartItems;
    private Long couponId; // optional

    public static class CartItemInfo {
        private Long cartItemId;
        private Integer quantity;
    }
}
