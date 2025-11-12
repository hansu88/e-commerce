package com.hhplus.ecommerce.application.usecase;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.command.coupon.UseCouponCommand;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class IssueCouponUseCaseTest {

    @Autowired


    private IssueCouponUseCase issueCouponUseCase;
    @Autowired

    private UseCouponUseCase useCouponUseCase;
    @Autowired

    private CouponRepository couponRepository;
    @Autowired

    private UserCouponRepository userCouponRepository;



    // Helper method to create a valid coupon with all required fields
    private Coupon createCoupon(String code, int totalQuantity, int issuedQuantity) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountAmount(1000);
        coupon.setTotalQuantity(totalQuantity);
        coupon.setIssuedQuantity(issuedQuantity);
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidUntil(LocalDateTime.now().plusDays(30));
        return coupon;
    }

    @Test
    @DisplayName("쿠폰 정상 발급하기")
    void issueCouponSuccess() {
        // Given - 총 발급 가량 1개인 쿠폰
        Coupon coupon = createCoupon("10PERCENT", 1, 0);
        couponRepository.save(coupon);

        // When - 쿠폰 발급
        IssueCouponCommand command = new IssueCouponCommand(1L, coupon.getId());
        UserCoupon userCoupon = issueCouponUseCase.execute(command);

        // Then - 정상 생성
        assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(1L),
                () -> assertThat(userCoupon.getCouponId()).isEqualTo(coupon.getId()),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE),
                () -> assertThat(userCoupon.getIssuedAt()).isBeforeOrEqualTo(LocalDateTime.now())
        );

        // 쿠폰 발급 수량 증가 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 한도 초과 시 예외")
    void issueCouponLimitExceeded() {
        // Given - 총 발급 수량 1개및 이미 발급 수량 1개
        Coupon coupon = createCoupon("10PERCENT", 1, 1);
        couponRepository.save(coupon);

        // When & Then - 쿠폰 한도 초과 에러 발생
        IssueCouponCommand command = new IssueCouponCommand(1L, coupon.getId());
        assertThatThrownBy(() -> issueCouponUseCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("쿠폰 발급 한도 초과");
    }

    @Test
    @DisplayName("쿠폰 정상 사용")
    void useCouponSuccess() {
        // Given -  총 발급 수량 1개 존재 , 현재 발급 수량 없음
        Coupon coupon = createCoupon("10PERCENT", 1, 0);
        couponRepository.save(coupon);

        IssueCouponCommand issueCommand = new IssueCouponCommand(1L, coupon.getId());
        UserCoupon userCoupon = issueCouponUseCase.execute(issueCommand);

        // When - 사용자에게 쿠폰 발급
        UseCouponCommand useCommand = new UseCouponCommand(userCoupon.getId());
        useCouponUseCase.execute(useCommand);

        // Then - 발급된 쿠폰상태가 전체 쿠폰테이블에서는 USED 되어있어야 함
        UserCoupon updated = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertAll(
                () -> assertThat(updated.getStatus()).isEqualTo(UserCouponStatus.USED),
                () -> assertThat(updated.getUsedAt()).isBeforeOrEqualTo(LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 사용 시 예외")
    void useCouponAlreadyUsed() {
        // Given - 쿠폰 발급 및 사용 진행
        Coupon coupon = createCoupon("10PERCENT", 1, 0);
        couponRepository.save(coupon);

        IssueCouponCommand issueCommand = new IssueCouponCommand(1L, coupon.getId());
        UserCoupon userCoupon = issueCouponUseCase.execute(issueCommand);

        UseCouponCommand useCommand = new UseCouponCommand(userCoupon.getId());
        useCouponUseCase.execute(useCommand);

        // When & Then - 사용한 쿠폰 사용 시 에러 발생
        assertThatThrownBy(() -> useCouponUseCase.execute(useCommand))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용된 쿠폰입니다.");
    }
}
