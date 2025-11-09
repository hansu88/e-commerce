package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.IssueCouponCommand;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 쿠폰 발급 UseCase
 * - 동시성 제어 적용 (Coupon ID별 Lock)
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    // Coupon ID별 Lock 객체 관리
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public UserCoupon execute(IssueCouponCommand command) {
        // Coupon ID별 Lock 객체 획득 (없으면 생성)
        Object lock = lockMap.computeIfAbsent(command.getCouponId(), k -> new Object());

        synchronized (lock) {
            Coupon coupon = couponRepository.findById(command.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

            // 발급 한도 체크
            if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
                throw new IllegalStateException("쿠폰 발급 한도 초과");
            }

            // 발급 수량 증가
            coupon.setIssuedQuantity(coupon.getIssuedQuantity() + 1);
            couponRepository.save(coupon);

            // UserCoupon 생성 및 저장
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(command.getUserId());
            userCoupon.setCouponId(command.getCouponId());
            userCoupon.setIssuedAt(LocalDateTime.now());
            userCoupon.setStatus(UserCouponStatus.AVAILABLE);

            return userCouponRepository.save(userCoupon);
        }
    }
}
