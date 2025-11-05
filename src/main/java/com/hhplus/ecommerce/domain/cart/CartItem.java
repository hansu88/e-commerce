package com.hhplus.ecommerce.domain.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 장바구니 항목 Entity
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long id;
    private Long cartId;
    private Long productOptionId;
    private Integer quantity;

}