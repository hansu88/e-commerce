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
        // Mock 응답
        return Map.of("cartId", 1L);
    }

    @GetMapping
    public List<CartItemResponseDto> getCart(@RequestParam Long uid) {
        // Mock 데이터
        CartItemResponseDto.ProductOption option1 = new CartItemResponseDto.ProductOption(101L, "Red", "M");
        CartItemResponseDto.ProductOption option2 = new CartItemResponseDto.ProductOption(102L, "Blue", "L");

        CartItemResponseDto item1 = new CartItemResponseDto(1L, option1, 2);
        CartItemResponseDto item2 = new CartItemResponseDto(2L, option2, 1);

        return List.of(item1, item2);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable Long id) {
        // Mock 삭제 처리
    }
}