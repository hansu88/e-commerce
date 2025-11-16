package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.coupon.UseCouponCommand;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 사용 UseCase
 */
@Component
@RequiredArgsConstructor
public class UseCouponUseCase {

    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void execute(UseCouponCommand command) {
        UserCoupon userCoupon = userCouponRepository.findById(command.getUserCouponId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        // Entity 메서드 사용 (비즈니스 규칙은 Entity에서 검증)
        userCoupon.use();

        userCouponRepository.save(userCoupon);
    }
}
