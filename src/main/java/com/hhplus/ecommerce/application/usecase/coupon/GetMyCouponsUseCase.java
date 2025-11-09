package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.application.command.GetMyCouponsCommand;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 내 쿠폰 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetMyCouponsUseCase {

    private final UserCouponRepository userCouponRepository;

    public List<UserCoupon> execute(GetMyCouponsCommand command) {
        return userCouponRepository.findByUserId(command.getUserId());
    }
}
