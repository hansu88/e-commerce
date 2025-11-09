package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @deprecated Use UseCase pattern instead:
 * - {@link com.hhplus.ecommerce.application.usecase.cart.AddCartItemUseCase} for adding cart item
 * - {@link com.hhplus.ecommerce.application.usecase.cart.GetCartItemsUseCase} for getting cart items
 * - {@link com.hhplus.ecommerce.application.usecase.cart.DeleteCartItemUseCase} for deleting cart item
 */
@Deprecated
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public CartItem addCartItem(Long userId, Long productOptionId, Integer quantity) {
        // 사용자의 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });

        // 장바구니 아이템 생성
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setProductOptionId(productOptionId);
        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));
        
        return cartItemRepository.findByCartId(cart.getId());
    }

    public void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));
        
        cartItemRepository.delete(cartItem);
    }
}
