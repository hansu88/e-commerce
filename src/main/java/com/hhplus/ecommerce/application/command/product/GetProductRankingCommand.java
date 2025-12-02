package com.hhplus.ecommerce.application.command.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 상품 랭킹 조회 Command
 */
@Getter
@AllArgsConstructor
public class GetProductRankingCommand {

    private int count;  // 조회할 개수 (예: TOP 5)

    public void validate() {
        if (count <= 0) {
            throw new IllegalArgumentException("조회 개수는 1 이상이어야 합니다.");
        }
        if (count > 100) {
            throw new IllegalArgumentException("조회 개수는 최대 100개까지입니다.");
        }
    }
}
