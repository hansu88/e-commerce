package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.RestoreCouponCommand;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 복구 UseCase (주문 취소 시)
 */
@Component
@RequiredArgsConstructor
public class RestoreCouponUseCase {

    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void execute(RestoreCouponCommand command) {
        UserCoupon userCoupon = userCouponRepository.findById(command.getUserCouponId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        if (userCoupon.getStatus() != UserCouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 복구할 수 있습니다.");
        }

        // 비즈니스 메서드 사용
        userCoupon.restore();
        userCouponRepository.save(userCoupon);
    }
}
