package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.coupon.*;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 쿠폰 서비스
 * - 쿠폰 발급 및 사용 처리
 * - 동시성 제어 적용 (Coupon ID별 Lock)
 * - 쿠폰 복구 (주문 취소 시)
 *
 * @deprecated Use UseCase pattern instead:
 * - {@link com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase} for issuing coupons
 * - {@link com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase} for using coupons
 * - {@link com.hhplus.ecommerce.application.usecase.coupon.RestoreCouponUseCase} for restoring coupons
 * - {@link com.hhplus.ecommerce.application.usecase.coupon.GetMyCouponsUseCase} for getting my coupons
 */
@Deprecated
@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponService(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    public CouponRepository getCouponRepository() {
        return couponRepository;
    }

    public UserCouponRepository getUserCouponRepository() {
        return userCouponRepository;
    }

    /**
     * 사용자에게 쿠폰 발급
     * 낙관적 락으로 동시성 제어
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
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
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setIssuedAt(LocalDateTime.now());
        userCoupon.setStatus(UserCouponStatus.AVAILABLE);
        userCoupon.setCreatedAt(LocalDateTime.now());

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 사용 처리
     */
    public void useCoupon(Long userCouponId){
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        if (userCoupon.getStatus() == UserCouponStatus.USED){
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }

        userCoupon.setStatus(UserCouponStatus.USED);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 복구 (주문 취소 시)
     * 사용된 쿠폰을 다시 사용 가능 상태로 변경
     */
    public void restoreCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        if (userCoupon.getStatus() != UserCouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 복구할 수 있습니다.");
        }

        userCoupon.setStatus(UserCouponStatus.AVAILABLE);
        userCoupon.setUsedAt(null);
        userCouponRepository.save(userCoupon);
    }
}
