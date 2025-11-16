package com.hhplus.ecommerce.application.command.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetPopularProductsCommand {
    private final int days;
    private final int limit;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (days <= 0 || days > 30) throw new IllegalArgumentException("days must be 1~30");
        if (limit <= 0 || limit > 100) throw new IllegalArgumentException("limit must be 1~100");
    }
}
