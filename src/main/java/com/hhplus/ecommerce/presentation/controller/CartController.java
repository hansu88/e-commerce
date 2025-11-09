package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.AddCartItemCommand;
import com.hhplus.ecommerce.application.command.DeleteCartItemCommand;
import com.hhplus.ecommerce.application.command.GetCartItemsCommand;
import com.hhplus.ecommerce.application.usecase.cart.AddCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.cart.DeleteCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.cart.GetCartItemsUseCase;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.presentation.dto.request.CartAddRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.CartItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final AddCartItemUseCase addCartItemUseCase;
    private final GetCartItemsUseCase getCartItemsUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;

    @PostMapping
    public ResponseEntity<Map<String, Long>> addCart(@RequestBody CartAddRequestDto request) {
        AddCartItemCommand command = new AddCartItemCommand(
                request.getUserId(),
                request.getProductOptionId(),
                request.getQuantity()
        );
        CartItem cartItem = addCartItemUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("cartItemId", cartItem.getId()));
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCart(@RequestParam Long uid) {
        GetCartItemsCommand command = new GetCartItemsCommand(uid);
        List<CartItem> cartItems = getCartItemsUseCase.execute(command);

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
        DeleteCartItemCommand command = new DeleteCartItemCommand(id);
        deleteCartItemUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
