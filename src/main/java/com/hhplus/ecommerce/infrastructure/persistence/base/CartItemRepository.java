package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.cart.CartItem;

import java.util.List;
import java.util.Optional;
/**
 * 장바구니 아이템(CartItem) Repository 인터페이스
 */
public interface CartItemRepository {

     /**
      * 장바구니 아이템 저장 (데이터 저장)
      */
     CartItem save(CartItem cartItem);

     /**
      * ID로 장바구니 아이템 조회 (GET /api/cart-items/{id})
      */
     Optional<CartItem> findById(Long id);

     /**
      * 특정 장바구니(cartId)에 속한 모든 아이템 조회
      */
     List<CartItem> findByCartId(Long cartId);

     /**
      * 모든 장바구니 아이템 조회 (GET /api/cart-items)
      */
     List<CartItem> findAll();

     /**
      * 장바구니 아이템 삭제
      */
     void delete(CartItem cartItem);
}