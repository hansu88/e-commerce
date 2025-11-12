package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.cart.DeleteCartItemCommand;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니 아이템 삭제 UseCase
 */
@Component
@RequiredArgsConstructor
public class DeleteCartItemUseCase {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public void execute(DeleteCartItemCommand command) {
        CartItem cartItem = cartItemRepository.findById(command.getCartItemId())
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        cartItemRepository.delete(cartItem);
    }
}
