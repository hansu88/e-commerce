package com.hhplus.ecommerce.application.command.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelOrderCommand {
    private final Long orderId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
    }

}
