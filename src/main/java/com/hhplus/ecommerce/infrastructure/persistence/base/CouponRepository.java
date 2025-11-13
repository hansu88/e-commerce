package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 전체 쿠폰(Coupon) 관리 Repository 인터페이스
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
}
