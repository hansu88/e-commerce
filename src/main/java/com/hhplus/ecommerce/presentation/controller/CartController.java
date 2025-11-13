package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.cart.AddCartItemCommand;
import com.hhplus.ecommerce.application.command.cart.DeleteCartItemCommand;
import com.hhplus.ecommerce.application.command.cart.GetCartItemsCommand;
import com.hhplus.ecommerce.application.usecase.cart.AddCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.cart.DeleteCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.cart.GetCartItemsUseCase;
import com.hhplus.ecommerce.presentation.dto.request.CartAddRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.cart.CartItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final AddCartItemUseCase addCartItemUseCase;
    private final GetCartItemsUseCase getCartItemsUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;

    /** 장바구니에 아이템 추가 */
    @PostMapping
    public ResponseEntity<Map<String, Long>> addCart(@RequestBody CartAddRequestDto request) {
        AddCartItemCommand command = new AddCartItemCommand(
                request.getUserId(),
                request.getProductOptionId(),
                request.getQuantity()
        );
        command.validate();

        var cartItem = addCartItemUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("cartItemId", cartItem.getId()));
    }

    /** 사용자의 장바구니 아이템 조회 */
    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCart(@RequestParam Long userId) {
        GetCartItemsCommand command = new GetCartItemsCommand(userId);
        command.validate();

        // GetCartItemsUseCase에서 이미 CartItemResponseDto 리스트를 반환하도록 수정했음
        List<CartItemResponseDto> response = getCartItemsUseCase.execute(command);

        return ResponseEntity.ok(response);
    }

    /** 장바구니 아이템 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long id) {
        DeleteCartItemCommand command = new DeleteCartItemCommand(id);
        command.validate();
        deleteCartItemUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
