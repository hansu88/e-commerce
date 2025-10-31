package com.hhplus.ecommerce.controller;

import com.hhplus.ecommerce.cart.dto.CartAddRequestDto;
import com.hhplus.ecommerce.cart.dto.CartItemResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    @PostMapping
    public Map<String, Long> addCart(@RequestBody CartAddRequestDto request) {
        return Map.of("cartId", 1L); // Mock cartId
    }

    @GetMapping
    public List<CartItemResponseDto> getCart(@RequestParam Long uid) {
        return List.of(new CartItemResponseDto(/*...*/));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable Long id) {
        // 삭제 mock
    }
}