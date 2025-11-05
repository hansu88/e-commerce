package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.coupon.*;
import com.hhplus.ecommerce.domain.order.OrderRepository;

import java.time.LocalDateTime;

public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponService(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    /**
     *  사용자에게 쿠폰 발급
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰 발급 한도 초과");
        }

        coupon.setIssuedQuantity(coupon.getIssuedQuantity() + 1);
        couponRepository.save(coupon);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setIssuedAt(LocalDateTime.now());
        userCoupon.setStatus(UserCouponStatus.AVAILABLE);

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 사용 처리 관련
     */
    public void useCoupon(Long userCouponId){
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        if (userCoupon.getStatus() == UserCouponStatus.USED){
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }

        userCoupon.setStatus(UserCouponStatus.USED);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(userCoupon);
    }

}
