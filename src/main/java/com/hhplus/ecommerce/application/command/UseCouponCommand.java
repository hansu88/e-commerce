package com.hhplus.ecommerce.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UseCouponCommand {
    private final Long userCouponId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (userCouponId == null) {
            throw new IllegalArgumentException("userCouponId must not be null");
        }
    }
}
