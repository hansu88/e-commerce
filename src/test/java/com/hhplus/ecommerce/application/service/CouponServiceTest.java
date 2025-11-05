package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryCouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryUserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class CouponServiceTest {


    private CouponService couponService;
    private InMemoryCouponRepository couponRepository;
    private InMemoryUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        couponService = new CouponService(couponRepository, userCouponRepository);
    }

    @Test
    @DisplayName("쿠폰 정상 발급하기")
    void issueCouponSuccess() {
        // Given - 총 발급 가량 1개인 쿠폰
        Coupon coupon = new Coupon();
        coupon.setCode("10PERCENT");
        coupon.setTotalQuantity(1);
        coupon.setIssuedQuantity(0);
        couponRepository.save(coupon);

        // When - 쿠폰 발급
        UserCoupon userCoupon = couponService.issueCoupon(1L, coupon.getId());

        // Then - 정상 생성
        assertThat(userCoupon.getUserId()).isEqualTo(1L);
        assertThat(userCoupon.getCouponId()).isEqualTo(coupon.getId());
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getIssuedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        // 쿠폰 발급 수량 증가 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 한도 초과 시 예외")
    void issueCouponLimitExceeded() {
        // Given - 총 발급 수량 1개및 이미 발급 수량 1개
        Coupon coupon = new Coupon();
        coupon.setCode("10PERCENT");
        coupon.setTotalQuantity(1);
        coupon.setIssuedQuantity(1);
        couponRepository.save(coupon);

        // When & Then - 쿠폰 한도 초과 에러 발생
        assertThatThrownBy(() -> couponService.issueCoupon(1L, coupon.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("쿠폰 발급 한도 초과");
    }

    @Test
    @DisplayName("쿠폰 정상 사용")
    void useCouponSuccess() {
        // Given -  총 발급 수량 1개 존재 , 현재 발급 수량 없음
        Coupon coupon = new Coupon();
        coupon.setCode("10PERCENT");
        coupon.setTotalQuantity(1);
        coupon.setIssuedQuantity(0);
        couponRepository.save(coupon);

        UserCoupon userCoupon = couponService.issueCoupon(1L, coupon.getId());

        // When - 사용자에게 쿠폰 발급
        couponService.useCoupon(userCoupon.getId());

        // Then - 발급된 쿠폰상태가 전체 쿠폰테이블에서는 USED 되어있어야 함
        UserCoupon updated = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(updated.getUsedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 사용 시 예외")
    void useCouponAlreadyUsed() {
        // Given - 쿠폰 발급 및 사용 진행
        Coupon coupon = new Coupon();
        coupon.setCode("10PERCENT");
        coupon.setTotalQuantity(1);
        coupon.setIssuedQuantity(0);
        couponRepository.save(coupon);

        UserCoupon userCoupon = couponService.issueCoupon(1L, coupon.getId());
        couponService.useCoupon(userCoupon.getId());

        // When & Then - 사용한 쿠폰 사용 시 에러 발생
        assertThatThrownBy(() -> couponService.useCoupon(userCoupon.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용된 쿠폰입니다.");
    }
}