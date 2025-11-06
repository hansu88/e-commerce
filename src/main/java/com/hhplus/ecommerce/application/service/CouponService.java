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
 */
@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    
    // Coupon ID별 Lock 객체 관리
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public CouponService(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    /**
     * 사용자에게 쿠폰 발급
     * 동시성 제어: Coupon ID별 synchronized 블록 적용
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급된 UserCoupon
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // Coupon ID별 Lock 객체 획득 (없으면 생성)
        Object lock = lockMap.computeIfAbsent(couponId, k -> new Object());
        
        synchronized (lock) {
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

            return userCouponRepository.save(userCoupon);
        }
    }

    /**
     * 쿠폰 사용 처리
     * @param userCouponId 사용자 쿠폰 ID
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
     * 쿠폰 사용 및 할인 금액 반환
     * - UserCoupon 조회 -> Coupon 조회 -> 할인 금액 가져오기 -> 쿠폰 사용 처리
     * - OrderService가 CouponRepository를 직접 의존하지 않도록 캡슐화
     * @param userCouponId 사용자 쿠폰 ID
     * @return 할인 금액
     */
    public int useCouponAndGetDiscount(Long userCouponId) {
        // UserCoupon 조회
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다: " + userCouponId));

        // Coupon 조회하여 할인 금액 가져오기
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + userCoupon.getCouponId()));

        // 쿠폰 사용 처리
        useCoupon(userCouponId);

        return coupon.getDiscountAmount();
    }

    /**
     * 쿠폰 복구 (주문 취소 시)
     * - 사용된 쿠폰을 다시 사용 가능 상태로 변경
     * @param userCouponId 사용자 쿠폰 ID
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
