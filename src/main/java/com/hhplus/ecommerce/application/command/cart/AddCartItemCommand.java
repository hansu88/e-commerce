package com.hhplus.ecommerce.application.command.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddCartItemCommand {
    private final Long userId;
    private final Long productOptionId;
    private final Integer quantity;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (productOptionId == null) {
            throw new IllegalArgumentException("상품 옵션 ID는 필수입니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }
}
