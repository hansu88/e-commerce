package com.hhplus.ecommerce.domain.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 장바구니 Entity
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Cart {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
}
