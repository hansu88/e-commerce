package com.hhplus.ecommerce.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon coupon);
    Optional<UserCoupon> findById(Long id);
    List<UserCoupon> findAll();
}
