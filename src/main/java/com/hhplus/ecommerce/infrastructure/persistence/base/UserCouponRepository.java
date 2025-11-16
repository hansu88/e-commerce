package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * 만료 대상 UserCoupon 조회 (최적화)
     * - AVAILABLE 상태이면서 Coupon의 validUntil이 지난 것
     * - JOIN으로 한 번에 조회하여 N+1 문제 해결
     * - FK 없이 coupon_id 컬럼으로 JOIN
     */
    @Query(value = "SELECT uc.* FROM user_coupons uc " +
            "INNER JOIN coupons c ON uc.coupon_id = c.id " +
            "WHERE uc.status = 'AVAILABLE' " +
            "AND c.valid_until < :now",
            nativeQuery = true)
    List<UserCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);
}
