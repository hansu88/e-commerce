package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.service.coupon.CouponService;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 UseCase (Redis 선착순 + DB 저장)
 *
 * STEP 14 개선:
 * - Redis로 선착순 체크 (빠름!)
 * - 통과 시에만 DB 저장
 * - 한도 초과 시 즉시 실패 (DB 접근 안 함)
 *
 * Before (비관적 락):
 * - 100명 신청 → 100번 DB 쿼리
 * - 락 대기로 느림
 *
 * After (Redis 선착순):
 * - 100명 신청 (한도 10장)
 * - Redis 체크: 100번 (빠름!)
 * - DB 저장: 10번만 (통과한 사람만)
 *
 * 흐름:
 * 1. Redis 선착순 체크 (CouponService)
 *    - 중복 발급 체크
 *    - 발급 수량 증가 (INCR)
 *    - 한도 체크
 * 2. 통과 시 DB 저장
 *    - UserCoupon INSERT
 *    - Coupon.issuedQuantity 증가 (동기화)
 * 3. 실패 시 즉시 리턴
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponService couponService;

    public UserCoupon execute(IssueCouponCommand command) {
        // 1. 쿠폰 정보 조회 (READ ONLY)
        Coupon coupon = couponRepository.findById(command.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        // 2. Redis 선착순 체크 (빠름!)
        // - 중복 발급 체크
        // - 발급 수량 증가 (INCR)
        // - 한도 체크
        boolean canIssue = couponService.tryIssueCoupon(
                command.getCouponId(),
                command.getUserId(),
                coupon.getTotalQuantity()
        );

        if (!canIssue) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 3. Redis 통과 시 DB 저장
        return saveToDatabase(command, coupon);
    }

    /**
     * DB에 쿠폰 발급 정보 저장
     * - Redis 선착순 통과 후에만 실행
     */
    @Transactional
    protected UserCoupon saveToDatabase(IssueCouponCommand command, Coupon coupon) {
        try {
            // 1. Coupon 발급 수량 증가 (Redis와 동기화)
            coupon.increaseIssuedQuantity();
            couponRepository.save(coupon);

            // 2. UserCoupon 생성
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(command.getUserId())
                    .couponId(coupon.getId())
                    .issuedAt(LocalDateTime.now())
                    .status(UserCouponStatus.AVAILABLE)
                    .build();

            return userCouponRepository.save(userCoupon);

        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약조건 위반 (중복 발급)
            // Redis에서 체크했지만 드물게 발생 가능
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.", e);
        }
    }
}