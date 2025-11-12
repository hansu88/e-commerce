package com.hhplus.ecommerce.concurrency;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 쿠폰 발급 동시성 테스트
 * - 여러 사용자가 동시에 선착순 쿠폰을 발급받을 때 정확히 한도만큼만 발급되는지 검증
 */
@SpringBootTest
public class CouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon createCoupon(String code, int discountAmount, int totalQuantity, int issuedQuantity) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountAmount(discountAmount);
        coupon.setTotalQuantity(totalQuantity);
        coupon.setIssuedQuantity(issuedQuantity);
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidUntil(LocalDateTime.now().plusDays(30));
        return couponRepository.save(coupon);
    }

    @Test
    @DisplayName("동시성 테스트 1: 쿠폰 100개, 동시에 100명 요청")
    void concurrentCouponIssuance_ExactLimit() throws InterruptedException {
        Coupon savedCoupon = createCoupon("CODE1", 5000, 100, 0);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<Long> issuedUserIds = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    IssueCouponCommand command = new IssueCouponCommand(userId, savedCoupon.getId());
                    UserCoupon userCoupon = issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                    issuedUserIds.add(userCoupon.getUserId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(100),
                () -> assertThat(issuedUserIds.size()).isEqualTo(100),
                () -> assertThat(userCouponRepository.countByCouponId(savedCoupon.getId())).isEqualTo(100)
        );

        System.out.println("===== 동시성 테스트 1 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 발급 수량: " + updatedCoupon.getIssuedQuantity());
        System.out.println("실제 발급된 UserCoupon 수: " + userCouponRepository.countByCouponId(savedCoupon.getId()));
        System.out.println("=============================");
    }

    @Test
    @DisplayName("동시성 테스트 2: 쿠폰 100개, 동시 요청 200개 - 100개만 발급, 100개 실패")
    void concurrentCouponIssuance_ExceedLimit() throws InterruptedException {
        // Given - 쿠폰 100개 생성
        Coupon coupon = createCoupon("LIMIT100", 5000, 100, 0);
        Coupon savedCoupon = couponRepository.save(coupon);

        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 200명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    IssueCouponCommand command = new IssueCouponCommand(userId, savedCoupon.getId());
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 정확히 100개만 발급, 100개는 실패해야 함
        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(100),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(100L)
        );

        System.out.println("===== 동시성 테스트 2 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("최종 발급 수량: " + updatedCoupon.getIssuedQuantity() + "개");
        System.out.println("=============================");
    }

    @Test
    @DisplayName("동시성 테스트 3: 쿠폰 50개, 동시 요청 100개 - Race Condition 방지")
    void concurrentCouponIssuance_RaceConditionPrevention() throws InterruptedException {
        Coupon savedCoupon = createCoupon("LIMIT50", 10000, 50, 0);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<Long> issuedUserIds = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    IssueCouponCommand command = new IssueCouponCommand(userId, savedCoupon.getId());
                    UserCoupon userCoupon = issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                    issuedUserIds.add(userCoupon.getUserId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(50),
                () -> assertThat(failCount.get()).isEqualTo(50),
                () -> assertThat(issuedUserIds.size()).isEqualTo(50),
                () -> assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(50),
                () -> assertThat(userCouponRepository.countByCouponId(savedCoupon.getId())).isEqualTo(50L)
        );

        System.out.println("===== 동시성 테스트 3 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 발급 수량: " + updatedCoupon.getIssuedQuantity());
        System.out.println("실제 발급된 UserCoupon 수: " + userCouponRepository.countByCouponId(savedCoupon.getId()));
        System.out.println("=============================");
    }
}