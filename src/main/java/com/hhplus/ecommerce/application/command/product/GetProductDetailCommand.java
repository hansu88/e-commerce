package com.hhplus.ecommerce.application.command.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetProductDetailCommand {
    private final Long productId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (productId == null) throw new IllegalArgumentException("productId must not be null");
    }
}
