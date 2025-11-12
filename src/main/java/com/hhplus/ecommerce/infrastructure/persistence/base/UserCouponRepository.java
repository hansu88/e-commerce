package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 사용자 쿠폰(UserCoupon) Repository 인터페이스
 */
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserId(Long userId);

    /**
     *  추후구현
     * @param couponId
     * @return
     */
    @Query("SELECT COUNT(u) FROM UserCoupon u WHERE u.couponId = :couponId")
    long countByCouponId(Long couponId);

    /**
     * 추후 구현
     * @param userId
     * @param id
     * @return
     */
    boolean existsByUserIdAndCouponId(Long userId, Long id);
}
