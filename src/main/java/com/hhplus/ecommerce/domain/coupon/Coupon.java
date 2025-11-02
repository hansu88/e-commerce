package com.hhplus.ecommerce.domain.coupon;

import java.time.LocalDateTime;

/**
 * 쿠폰 Entity
 */
public class Coupon {
    private Long id;
    private String code;
    private Integer discountAmount;
    private Integer totalQuantity;      // 총 발급 가능 수량
    private Integer issuedQuantity;     // 현재까지 발급된 수량
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    // 생성자
    public Coupon(Long id, String code, Integer discountAmount, Integer totalQuantity,
                  Integer issuedQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.id = id;
        this.code = code;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    // Getter
    public Long getId() { return id; }
    public String getCode() { return code; }
    public Integer getDiscountAmount() { return discountAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getIssuedQuantity() { return issuedQuantity; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }

    // Setter
    public void setId(Long id) { this.id = id; }
    public void setIssuedQuantity(Integer issuedQuantity) { this.issuedQuantity = issuedQuantity; }
}
