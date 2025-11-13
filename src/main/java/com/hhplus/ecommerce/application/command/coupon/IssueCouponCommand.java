package com.hhplus.ecommerce.application.command.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssueCouponCommand {
    private final Long userId;
    private final Long couponId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (couponId == null) {
            throw new IllegalArgumentException("쿠폰 ID는 필수입니다.");
        }
    }
}
