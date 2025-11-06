package com.hhplus.ecommerce.infrastructure.persistence.base;
import com.hhplus.ecommerce.domain.coupon.Coupon;

import java.util.List;
import java.util.Optional;

/**
 * 전체 쿠폰(Coupon) 관리 Repository 인터페이스
 */
public interface CouponRepository {

    /**
     * 쿠폰 저장 (데이터 저장)
     */
    Coupon save(Coupon coupon);

    /**
     * ID로 쿠폰 조회 (GET /api/coupons/{id})
     */
    Optional<Coupon> findById(Long id);

    /**
     * 모든 쿠폰 조회 (GET /api/coupons)
     */
    List<Coupon> findAll();
}
