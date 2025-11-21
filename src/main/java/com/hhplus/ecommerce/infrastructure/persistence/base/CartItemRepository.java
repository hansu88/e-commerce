package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 아이템(CartItem) Repository 인터페이스
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);

    /**
     * 장바구니에서 특정 상품 옵션 조회
     * - 장바구니 중복 추가 방지용
     * - uk_cart_product UNIQUE 제약조건과 함께 사용
     */
    Optional<CartItem> findByCartIdAndProductOptionId(Long cartId, Long productOptionId);
}