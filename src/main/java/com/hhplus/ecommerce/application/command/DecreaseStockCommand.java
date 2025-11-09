package com.hhplus.ecommerce.application.command;

import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DecreaseStockCommand {
    private final Long productOptionId;
    private final int quantity;
    private final StockChangeReason reason;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (productOptionId == null) {
            throw new IllegalArgumentException("상품 옵션 ID는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1개 이상이어야 합니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("재고 차감 사유는 필수입니다.");
        }
    }
}
