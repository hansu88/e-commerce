package com.hhplus.ecommerce.domain.coupon;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 Entity (발급된 쿠폰)
 */
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;       // 사용 일시 (nullable)
    private UserCouponStatus status;

    // 생성자
    public UserCoupon(Long id, Long userId, Long couponId, LocalDateTime issuedAt,
                      LocalDateTime usedAt, UserCouponStatus status) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
        this.status = status;
    }

    // Getter
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCouponId() { return couponId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public UserCouponStatus getStatus() { return status; }

    // Setter
    public void setId(Long id) { this.id = id; }
    public void setStatus(UserCouponStatus status) { this.status = status; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
