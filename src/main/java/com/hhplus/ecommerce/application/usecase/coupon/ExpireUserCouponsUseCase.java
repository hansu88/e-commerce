package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 만료 처리 UseCase
 * - 유효기간이 지난 쿠폰을 자동으로 EXPIRED 상태로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireUserCouponsUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    /**
     * 만료된 쿠폰 일괄 처리
     * @return 만료 처리된 쿠폰 개수
     */
    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        log.info("쿠폰 만료 처리 시작: {}", now);

        // 사용 가능 상태의 모든 사용자 쿠폰 조회
        List<UserCoupon> availableCoupons = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.AVAILABLE)
                .toList();

        int expiredCount = 0;

        for (UserCoupon userCoupon : availableCoupons) {
            // 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId()).orElse(null);

            if (coupon != null && coupon.getValidUntil().isBefore(now)) {
                // 유효기간이 지난 경우 만료 처리
                userCoupon.expire();
                expiredCount++;
            }
        }

        log.info("쿠폰 만료 처리 완료: {} 개", expiredCount);
        return expiredCount;
    }
}
