package com.hhplus.ecommerce.application.command.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PayOrderCommand {
    private final Long orderId;
    private final String paymentMethod;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (orderId == null) throw new IllegalArgumentException("주문 ID는 필수입니다.");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) throw new IllegalArgumentException("결제 수단은 필수입니다.");
    }
}
