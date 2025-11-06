package com.hhplus.ecommerce.domain.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 쿠폰 Entity
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Coupon {
    private Long id;
    private String name;                // 쿠폰 이름
    private String code;
    private Integer discountAmount;     // 할인 금액 (고정 금액)
    private Integer totalQuantity;      // 총 발급 가능 수량
    private Integer issuedQuantity;     // 현재까지 발급된 수량
    private LocalDate validFrom;
    private LocalDate validUntil;
}
