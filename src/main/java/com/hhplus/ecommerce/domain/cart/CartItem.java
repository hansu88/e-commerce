package com.hhplus.ecommerce.domain.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 장바구니 항목 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long id;
    private Long cartId;
    private Long productOptionId;
    private Integer quantity;

}