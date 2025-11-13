package com.hhplus.ecommerce.application.usecase.cart;

import com.hhplus.ecommerce.application.command.cart.GetCartItemsCommand;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
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
    private final ProductOptionRepository productOptionRepository;

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
                    // ProductOption 실제 조회
                    ProductOption productOption = productOptionRepository.findById(item.getProductOptionId())
                            .orElse(null);

                    String color = productOption != null ? productOption.getColor() : "Unknown";
                    String size = productOption != null ? productOption.getSize() : "Unknown";

                    var option = new CartItemResponseDto.ProductOption(
                            item.getProductOptionId(),
                            color,
                            size
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