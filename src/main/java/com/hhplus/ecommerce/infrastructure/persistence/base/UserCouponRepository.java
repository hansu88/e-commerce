package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.coupon.UserCoupon;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 쿠폰(UserCoupon) Repository 인터페이스
 */
public interface UserCouponRepository {

    /**
     * 사용자 쿠폰 저장 (데이터 저장)
     */
    UserCoupon save(UserCoupon coupon);

    /**
     * ID로 사용자 쿠폰 조회 (GET /api/user-coupons/{id})
     */
    Optional<UserCoupon> findById(Long id);

    /**
     * 모든 사용자 쿠폰 조회 (GET /api/user-coupons)
     */
    List<UserCoupon> findAll();

    /**
     * 특정 사용자가 가진 모든 쿠폰 조회
     */
    List<UserCoupon> findByUserId(Long userId);
}
