package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 UseCase
 * - 낙관적 락 (@Version) 사용
 * - OptimisticLockException 발생 시 최대 5회 재시도
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private static final int MAX_RETRIES = 30;

    public UserCoupon execute(IssueCouponCommand command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                return executeInternal(command);
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                try {
                    Thread.sleep(retryCount * 5L); // 점진적 back-off
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("쿠폰 발급 실패: 인터럽트", ie);
                }
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

        // 2. 사용자 이미 발급 여부 체크
        if (userCouponRepository.existsByUserIdAndCouponId(command.getUserId(), coupon.getId())) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 3. 재고 체크
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰 재고가 모두 소진되었습니다.");
        }

        // 4. 발급 수량 증가 (낙관적 락)
        coupon.setIssuedQuantity(coupon.getIssuedQuantity() + 1);
        couponRepository.saveAndFlush(coupon); // 즉시 DB 반영 + version 증가

        // 5. UserCoupon 생성
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(command.getUserId());
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setIssuedAt(LocalDateTime.now());
        userCoupon.setStatus(UserCouponStatus.AVAILABLE);

        return userCouponRepository.save(userCoupon);
    }
}