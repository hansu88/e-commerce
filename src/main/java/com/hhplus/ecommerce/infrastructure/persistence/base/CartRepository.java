package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.cart.Cart;

import java.util.List;
import java.util.Optional;
/**
 * 장바구니(Cart) Repository 인터페이스
 */
public interface CartRepository {

    /**
     * 장바구니 저장 (데이터 저장)
     */
    Cart save(Cart cart);

    /**
     * ID로 장바구니 조회 (GET /api/carts/{id})
     */
    Optional<Cart> findById(Long id);

    /**
     * 사용자 ID로 장바구니 조회 (GET /api/users/{userId}/cart)
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * 모든 장바구니 조회 (GET /api/carts)
     */
    List<Cart> findAll();
}
