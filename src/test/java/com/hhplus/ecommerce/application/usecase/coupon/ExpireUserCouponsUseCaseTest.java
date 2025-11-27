package com.hhplus.ecommerce.application.usecase.coupon;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ExpireUserCouponsUseCaseTest {

    @Autowired
    private ExpireUserCouponsUseCase expireUserCouponsUseCase;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("쿠폰 만료 처리 - 유효기간 지난 쿠폰 EXPIRED 상태로 변경")
    void expireCoupons_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 만료된 쿠폰
        Coupon expiredCoupon = createCoupon("EXPIRED001", 5000, now.minusDays(10), now.minusDays(1));
        UserCoupon userCoupon1 = createUserCoupon(1L, expiredCoupon.getId(), UserCouponStatus.AVAILABLE);

        // 아직 유효한 쿠폰
        Coupon validCoupon = createCoupon("VALID001", 3000, now.minusDays(1), now.plusDays(30));
        UserCoupon userCoupon2 = createUserCoupon(2L, validCoupon.getId(), UserCouponStatus.AVAILABLE);

        // When
        int expiredCount = expireUserCouponsUseCase.execute();

        // Then
        assertThat(expiredCount).isEqualTo(1);

        UserCoupon result1 = userCouponRepository.findById(userCoupon1.getId()).orElseThrow();
        assertThat(result1.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);

        UserCoupon result2 = userCouponRepository.findById(userCoupon2.getId()).orElseThrow();
        assertThat(result2.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰 만료 처리 - 이미 사용된 쿠폰은 만료 처리 안 함")
    void expireCoupons_SkipUsedCoupons() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        Coupon expiredCoupon = createCoupon("EXPIRED001", 5000, now.minusDays(10), now.minusDays(1));

        // 이미 사용된 쿠폰
        UserCoupon usedCoupon = createUserCoupon(1L, expiredCoupon.getId(), UserCouponStatus.USED);

        // When
        int expiredCount = expireUserCouponsUseCase.execute();

        // Then
        assertThat(expiredCount).isEqualTo(0);

        UserCoupon result = userCouponRepository.findById(usedCoupon.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(UserCouponStatus.USED); // 상태 유지
    }

    @Test
    @DisplayName("쿠폰 만료 처리 - 만료 대상 없을 때 0 반환")
    void expireCoupons_NoExpiredCoupons() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        Coupon validCoupon = createCoupon("VALID001", 3000, now.minusDays(1), now.plusDays(30));
        createUserCoupon(1L, validCoupon.getId(), UserCouponStatus.AVAILABLE);

        // When
        int expiredCount = expireUserCouponsUseCase.execute();

        // Then
        assertThat(expiredCount).isEqualTo(0);
    }

    @Test
    @DisplayName("쿠폰 만료 처리 - 여러 만료 쿠폰 일괄 처리")
    void expireCoupons_MultipleCoupons() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        Coupon expiredCoupon1 = createCoupon("EXPIRED001", 5000, now.minusDays(10), now.minusDays(1));
        Coupon expiredCoupon2 = createCoupon("EXPIRED002", 3000, now.minusDays(20), now.minusDays(5));

        createUserCoupon(1L, expiredCoupon1.getId(), UserCouponStatus.AVAILABLE);
        createUserCoupon(2L, expiredCoupon2.getId(), UserCouponStatus.AVAILABLE);
        createUserCoupon(3L, expiredCoupon1.getId(), UserCouponStatus.AVAILABLE);

        // When
        int expiredCount = expireUserCouponsUseCase.execute();

        // Then
        assertThat(expiredCount).isEqualTo(3);

        long expiredUserCoupons = userCouponRepository.findAll().stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.EXPIRED)
                .count();
        assertThat(expiredUserCoupons).isEqualTo(3);
    }

    // === Helper Methods ===

    private Coupon createCoupon(String code, int discountAmount,
                                 LocalDateTime validFrom, LocalDateTime validUntil) {
        Coupon coupon = Coupon.builder()
                .code(code)
                .discountAmount(discountAmount)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();
        return couponRepository.save(coupon);
    }

    private UserCoupon createUserCoupon(Long userId, Long couponId, UserCouponStatus status) {
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .issuedAt(LocalDateTime.now())
                .status(status)
                .build();
        return userCouponRepository.save(userCoupon);
    }
}
