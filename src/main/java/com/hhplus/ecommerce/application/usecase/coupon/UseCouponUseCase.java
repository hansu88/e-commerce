package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.UseCouponCommand;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 쿠폰 사용 UseCase
 */
@Component
@RequiredArgsConstructor
public class UseCouponUseCase {

    private final UserCouponRepository userCouponRepository;

    public void execute(UseCouponCommand command) {
        UserCoupon userCoupon = userCouponRepository.findById(command.getUserCouponId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }

        userCoupon.setStatus(UserCouponStatus.USED);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(userCoupon);
    }
}
