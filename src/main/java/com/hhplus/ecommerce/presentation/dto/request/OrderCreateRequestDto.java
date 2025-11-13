package com.hhplus.ecommerce.presentation.dto.request;

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
        private Long productOptionId;
        private Integer quantity;
        private Integer price;
    }
}
