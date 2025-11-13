package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 만료 처리 UseCase (개선)
 * - 기존: findAll() + 메모리 필터링 + N+1 문제
 * - 개선: JOIN 쿼리로 만료 대상만 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireUserCouponsUseCase {

    private final UserCouponRepository userCouponRepository;

    /**
     * 만료된 쿠폰 일괄 처리
     * @return 만료 처리된 쿠폰 개수
     */
    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        log.info("쿠폰 만료 처리 시작: {}", now);

        // JOIN 쿼리로 만료 대상 UserCoupon만 조회 (N+1 문제 해결)
        List<UserCoupon> expiredCoupons = userCouponRepository.findExpiredCoupons(now);

        // 만료 처리
        for (UserCoupon userCoupon : expiredCoupons) {
            userCoupon.expire();
        }

        int expiredCount = expiredCoupons.size();
        log.info("쿠폰 만료 처리 완료: {} 개", expiredCount);
        return expiredCount;
    }
}
