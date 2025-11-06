package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.service.CartService;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.presentation.dto.request.CartAddRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.CartItemResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> addCart(@RequestBody CartAddRequestDto request) {
        CartItem cartItem = cartService.addCartItem(
                request.getUserId(),
                request.getProductOptionId(),
                request.getQuantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("cartItemId", cartItem.getId()));
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCart(@RequestParam Long uid) {
        List<CartItem> cartItems = cartService.getCartItems(uid);

        List<CartItemResponseDto> response = cartItems.stream()
                .map(item -> new CartItemResponseDto(
                        item.getId(),
                        null, // ProductOption 정보는 추후 추가
                        item.getQuantity()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long id) {
        cartService.deleteCartItem(id);
        return ResponseEntity.noContent().build();
    }
}
