package com.hhplus.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderCreateRequestDto {
    private Long userId;
    private List<CartItemInfo> cartItems;
    private Long couponId; // optional

    @Getter
    @AllArgsConstructor
    public static class CartItemInfo {
        private Long cartItemId;
        private Integer quantity;
    }
}
