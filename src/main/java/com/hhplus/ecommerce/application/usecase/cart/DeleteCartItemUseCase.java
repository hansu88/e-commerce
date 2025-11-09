package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.DeleteCartItemCommand;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 장바구니 아이템 삭제 UseCase
 */
@Component
@RequiredArgsConstructor
public class DeleteCartItemUseCase {

    private final CartItemRepository cartItemRepository;

    public void execute(DeleteCartItemCommand command) {
        CartItem cartItem = cartItemRepository.findById(command.getCartItemId())
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        cartItemRepository.delete(cartItem);
    }
}
