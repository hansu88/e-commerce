package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.cart.AddCartItemCommand;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 장바구니에 아이템 추가 UseCase (동시성 제어)
 * - 같은 상품 중복 추가 시 수량 증가
 * - UNIQUE 제약조건(uk_cart_product)으로 DB 레벨 중복 방지
 */
@Component
@RequiredArgsConstructor
public class AddCartItemUseCase {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public CartItem execute(AddCartItemCommand command) {
        // 1. 사용자의 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByUserId(command.getUserId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(command.getUserId());
                    return cartRepository.save(newCart);
                });

        // 2. 이미 같은 상품이 장바구니에 있는지 확인
        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductOptionId(cart.getId(), command.getProductOptionId());

        if (existingItem.isPresent()) {
            // 2-1. 이미 존재 → 수량 증가
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + command.getQuantity());
            return cartItemRepository.save(item);
        } else {
            // 2-2. 존재하지 않음 → 새로 추가
            try {
                CartItem cartItem = new CartItem();
                cartItem.setCartId(cart.getId());
                cartItem.setProductOptionId(command.getProductOptionId());
                cartItem.setQuantity(command.getQuantity());

                return cartItemRepository.save(cartItem);
            } catch (DataIntegrityViolationException e) {
                // UNIQUE 제약조건 위반 (동시 INSERT 시도)
                // → 다시 조회해서 수량 증가
                CartItem item = cartItemRepository
                        .findByCartIdAndProductOptionId(cart.getId(), command.getProductOptionId())
                        .orElseThrow(() -> new IllegalStateException("장바구니 아이템 조회 실패"));

                item.setQuantity(item.getQuantity() + command.getQuantity());
                return cartItemRepository.save(item);
            }
        }
    }
}
