package com.hhplus.ecommerce.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequestDto {
    private Long userId;
    private List<CartItemInfo> cartItems;
    private Long couponId; // optional

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItemInfo {
        private Long cartItemId;
        private Long productOptionId;  // 추가
        private Integer quantity;
    }
}
