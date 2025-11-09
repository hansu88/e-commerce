package com.hhplus.ecommerce.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteCartItemCommand {
    private final Long cartItemId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (cartItemId == null) {
            throw new IllegalArgumentException("장바구니 아이템 ID는 필수입니다.");
        }
    }
}
