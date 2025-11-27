package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
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
 * 쿠폰 발급 UseCase (비관적 락 적용)
 *
 * 동시성 제어 전략:
 * - 비관적 락(PESSIMISTIC_WRITE) 사용
 * - SELECT ... FOR UPDATE로 행 단위 잠금
 * - 다른 트랜잭션은 락 해제 시까지 대기
 *
 * 비관적 락 선택 이유:
 * 1. 선착순 쿠폰 발급은 충돌이 매우 빈번한 시나리오
 * 2. 낙관적 락 사용 시 재시도 폭증으로 CPU 낭비
 * 3. DB 레벨에서 순차 처리가 더 효율적
 *
 * Before (낙관적 락):
 * - 100회 재시도 + 지수 백오프
 * - CPU: 높음 (재시도 반복)
 * - 코드 복잡도: 높음
 *
 * After (비관적 락):
 * - 재시도 불필요 (DB가 대기 처리)
 * - CPU: 낮음 (대기만)
 * - 코드 복잡도: 낮음
 *
 * 참고: 코치 피드백 반영
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public UserCoupon execute(IssueCouponCommand command) {
        try {
            // 1. 비관적 락으로 쿠폰 조회 (SELECT ... FOR UPDATE)
            Coupon coupon = couponRepository.findByIdWithPessimisticLock(command.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

            // 2. 재고 체크
            if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
                throw new IllegalStateException("쿠폰 발급 한도 초과");
            }

            // 3. 발급 수량 증가 (비즈니스 메서드)
            coupon.increaseIssuedQuantity();
            couponRepository.save(coupon);

            // 4. UserCoupon 생성 (UNIQUE 제약조건으로 중복 방지)
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(command.getUserId())
                    .couponId(coupon.getId())
                    .issuedAt(LocalDateTime.now())
                    .status(UserCouponStatus.AVAILABLE)
                    .build();

            return userCouponRepository.save(userCoupon);

        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약조건 위반 (중복 발급)
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.", e);
        }
    }
}