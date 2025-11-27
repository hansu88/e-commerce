package com.hhplus.ecommerce.infrastructure.scheduler;

import com.hhplus.ecommerce.application.usecase.coupon.ExpireUserCouponsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 관련 스케줄러
 * - 쿠폰 만료 처리
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CouponScheduler {

    private final ExpireUserCouponsUseCase expireUserCouponsUseCase;

    /**
     * 쿠폰 만료 처리
     * - 매시간 정각에 실행
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireCoupons() {
        try {
            log.info("=== 쿠폰 만료 처리 스케줄러 시작 ===");
            int expiredCount = expireUserCouponsUseCase.execute();
            log.info("=== 쿠폰 만료 처리 스케줄러 완료: {} 개 만료 ===", expiredCount);
        } catch (Exception e) {
            log.error("쿠폰 만료 처리 중 오류 발생", e);
        }
    }
}
