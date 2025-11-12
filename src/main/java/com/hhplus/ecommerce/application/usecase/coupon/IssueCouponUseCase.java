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
    private static final int MAX_RETRIES = 20;

    public UserCoupon execute(IssueCouponCommand command) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                return executeInternal(command);
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    throw new IllegalStateException("쿠폰 발급 한도 초과");
                }
                // 짧은 대기 후 재시도 (exponential backoff)
                try {
                    Thread.sleep(retryCount * 5L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("쿠폰 발급 한도 초과");
                }
            }
        }
        throw new IllegalStateException("쿠폰 발급 한도 초과");
    }

    @Transactional
    protected UserCoupon executeInternal(IssueCouponCommand command) {
        Coupon coupon = couponRepository.findById(command.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        // 발급 한도 체크
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalStateException("쿠폰 발급 한도 초과");
        }

        // 발급 수량 증가 (낙관적 락으로 동시성 제어)
        coupon.setIssuedQuantity(coupon.getIssuedQuantity() + 1);
        couponRepository.save(coupon);

        // UserCoupon 생성 및 저장
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(command.getUserId());
        userCoupon.setCouponId(command.getCouponId());
        userCoupon.setIssuedAt(LocalDateTime.now());
        userCoupon.setStatus(UserCouponStatus.AVAILABLE);
        userCoupon.setCreatedAt(LocalDateTime.now());

        return userCouponRepository.save(userCoupon);
    }
}
