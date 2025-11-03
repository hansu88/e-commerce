package com.hhplus.ecommerce.domain.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 쿠폰 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class Coupon {
    private Long id;
    private String code;
    private Integer discountAmount;
    private Integer totalQuantity;      // 총 발급 가능 수량
    private Integer issuedQuantity;     // 현재까지 발급된 수량
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

}
