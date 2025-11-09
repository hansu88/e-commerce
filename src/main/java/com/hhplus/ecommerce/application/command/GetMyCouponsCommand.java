package com.hhplus.ecommerce.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetMyCouponsCommand {
    private final Long userId;

    /**
     * Command 유효성 검증
     * @throws IllegalArgumentException 유효하지 않은 파라미터가 있을 경우
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }
}
