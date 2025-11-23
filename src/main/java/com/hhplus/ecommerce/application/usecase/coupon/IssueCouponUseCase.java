package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.common.util.RetryUtils;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 UseCase
 * - 낙관적 락 (@Version) 사용
 * - OptimisticLockException 발생 시 최대 100회 재시도 (지수 백오프)
 *
 * 재시도 전략:
 * - 최대 재시도: 100회
 * - 백오프: 지수 백오프 (1ms, 2ms, 4ms, 8ms, ..., 최대 100ms)
 * - 누적 최대 대기: 약 1.2초 (선형 백오프 대비 90% 감소)
 *
 * 재시도 횟수 근거:
 * - 쿠폰 선착순 발급은 충돌이 매우 빈번한 시나리오
 * - 높은 재시도 횟수로 사용자 경험 개선
 * - 향후 비관적 락으로 전환 예정 (STEP 2)
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private static final int MAX_RETRIES = 100;
    private static final long MAX_BACKOFF_MS = 100L;

    public UserCoupon execute(IssueCouponCommand command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                return executeInternal(command);
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                try {
                    RetryUtils.backoff(retryCount, MAX_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("쿠폰 발급 실패: 인터럽트", ie);
                }
            } catch (DataIntegrityViolationException e) {
                // UNIQUE 제약조건 위반 (중복 발급)
                throw new IllegalStateException("이미 발급받은 쿠폰입니다.", e);
            } catch (IllegalStateException e) {
                // 재고 없음 또는 이미 발급받은 경우 즉시 실패
                throw e;
            }
        }

        throw new IllegalStateException("쿠폰 발급 실패: 재시도 한도 초과");
    }

    @Transactional
    protected UserCoupon executeInternal(IssueCouponCommand command) {
        // 1. 최신 쿠폰 조회
        Coupon coupon = couponRepository.findById(command.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        // 2. 재고 체크 (중복 발급은 UNIQUE 제약조건으로 처리)
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰 발급 한도 초과");
        }

        // 3. 발급 수량 증가 (낙관적 락) - 비즈니스 메서드 사용
        coupon.increaseIssuedQuantity();
        couponRepository.saveAndFlush(coupon); // 즉시 DB 반영 + version 증가

        // 4. UserCoupon 생성 (UNIQUE 제약조건으로 중복 방지)
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(command.getUserId())
                .couponId(coupon.getId())
                .issuedAt(LocalDateTime.now())
                .status(UserCouponStatus.AVAILABLE)
                .build();

        return userCouponRepository.save(userCoupon);
    }
}