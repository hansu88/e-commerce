package com.hhplus.ecommerce.domain.coupon;

import java.util.List;
import java.util.Optional;

/**
 * 전체 쿠폰 관리
 */
public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long id);
    List<Coupon> findAll();
}
