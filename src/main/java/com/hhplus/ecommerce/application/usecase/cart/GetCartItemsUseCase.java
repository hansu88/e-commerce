package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.GetCartItemsCommand;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 사용자의 장바구니 아이템 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetCartItemsUseCase {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public List<CartItem> execute(GetCartItemsCommand command) {
        Cart cart = cartRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        return cartItemRepository.findByCartId(cart.getId());
    }
}
