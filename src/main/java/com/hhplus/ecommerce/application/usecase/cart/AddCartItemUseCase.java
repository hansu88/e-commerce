package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.AddCartItemCommand;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 장바구니에 아이템 추가 UseCase
 */
@Component
@RequiredArgsConstructor
public class AddCartItemUseCase {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartItem execute(AddCartItemCommand command) {
        // 사용자의 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByUserId(command.getUserId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(command.getUserId());
                    return cartRepository.save(newCart);
                });

        // 장바구니 아이템 생성
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setProductOptionId(command.getProductOptionId());
        cartItem.setQuantity(command.getQuantity());

        return cartItemRepository.save(cartItem);
    }
}
