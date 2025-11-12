package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.cart.GetCartItemsCommand;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import com.hhplus.ecommerce.presentation.dto.response.cart.CartItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자의 장바구니 아이템 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetCartItemsUseCase {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public List<CartItemResponseDto> execute(GetCartItemsCommand command) {
        Cart cart = cartRepository.findByUserId(command.getUserId())
                .orElse(null);

        if (cart == null) {
            return List.of(); // 빈 리스트 반환, 장바구니 자체가 없음
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        // CartItem -> CartItemResponseDto 변환
        return cartItems.stream()
                .map(item -> {
                    var option = new CartItemResponseDto.ProductOption(
                            item.getProductOptionId(), // ProductOption 엔티티에서 필요한 값 매핑
                            "color-placeholder",       // 실제 매핑 시 productOptionRepository에서 조회 가능
                            "size-placeholder"
                    );
                    return new CartItemResponseDto(
                            item.getId(),
                            option,
                            item.getQuantity()
                    );
                })
                .collect(Collectors.toList());
    }
}