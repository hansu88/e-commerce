package com.hhplus.ecommerce.application.service.coupon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CouponService 통합 테스트
 * - Redis Testcontainers를 사용한 독립적인 테스트 환경
 * - 선착순 쿠폰 발급 동시성 검증
 */
@SpringBootTest
@Testcontainers
@DisplayName("CouponService 통합 테스트")
class CouponServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CouponService couponService;

    private static final Long TEST_COUPON_ID = 1L;
    private static final int TOTAL_LIMIT = 100;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 쿠폰 초기화
        couponService.resetCouponIssued(TEST_COUPON_ID);
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 정리
        couponService.resetCouponIssued(TEST_COUPON_ID);
    }

    @Test
    @DisplayName("선착순 쿠폰 발급에 성공한다")
    void tryIssueCoupon_Success() {
        // given
        Long userId = 1L;

        // when
        boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, userId, TOTAL_LIMIT);

        // then
        assertThat(result).isTrue();
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(1);
        assertThat(couponService.isAlreadyIssued(TEST_COUPON_ID, userId)).isTrue();
    }

    @Test
    @DisplayName("중복 발급 시 예외가 발생한다")
    void tryIssueCoupon_Duplicate() {
        // given
        Long userId = 1L;
        couponService.tryIssueCoupon(TEST_COUPON_ID, userId, TOTAL_LIMIT);

        // when & then
        assertThatThrownBy(() -> couponService.tryIssueCoupon(TEST_COUPON_ID, userId, TOTAL_LIMIT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");

        // 발급 수량은 그대로 1
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("한도 초과 시 발급에 실패한다")
    void tryIssueCoupon_LimitExceeded() {
        // given - 10개 한도 쿠폰
        int limit = 10;
        for (long i = 1; i <= 10; i++) {
            couponService.tryIssueCoupon(TEST_COUPON_ID, i, limit);
        }

        // when - 11번째 시도
        boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, 11L, limit);

        // then
        assertThat(result).isFalse();
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 사용자가 순차적으로 발급받는다")
    void tryIssueCoupon_Multiple() {
        // given
        int count = 5;

        // when
        for (long i = 1; i <= count; i++) {
            boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, i, TOTAL_LIMIT);
            assertThat(result).isTrue();
        }

        // then
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(5);
        assertThat(couponService.getRemainingCount(TEST_COUPON_ID, TOTAL_LIMIT)).isEqualTo(95);
    }

    @Test
    @DisplayName("발급 수량과 남은 수량을 조회한다")
    void getIssuedCountAndRemaining() {
        // given
        couponService.tryIssueCoupon(TEST_COUPON_ID, 1L, TOTAL_LIMIT);
        couponService.tryIssueCoupon(TEST_COUPON_ID, 2L, TOTAL_LIMIT);
        couponService.tryIssueCoupon(TEST_COUPON_ID, 3L, TOTAL_LIMIT);

        // when
        int issuedCount = couponService.getIssuedCount(TEST_COUPON_ID);
        int remainingCount = couponService.getRemainingCount(TEST_COUPON_ID, TOTAL_LIMIT);

        // then
        assertThat(issuedCount).isEqualTo(3);
        assertThat(remainingCount).isEqualTo(97);
    }

    @Test
    @DisplayName("사용자 발급 여부를 확인한다")
    void isAlreadyIssued() {
        // given
        Long issuedUserId = 1L;
        Long notIssuedUserId = 2L;
        couponService.tryIssueCoupon(TEST_COUPON_ID, issuedUserId, TOTAL_LIMIT);

        // when
        boolean issued = couponService.isAlreadyIssued(TEST_COUPON_ID, issuedUserId);
        boolean notIssued = couponService.isAlreadyIssued(TEST_COUPON_ID, notIssuedUserId);

        // then
        assertThat(issued).isTrue();
        assertThat(notIssued).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 데이터를 초기화한다")
    void resetCouponIssued() {
        // given
        couponService.tryIssueCoupon(TEST_COUPON_ID, 1L, TOTAL_LIMIT);
        couponService.tryIssueCoupon(TEST_COUPON_ID, 2L, TOTAL_LIMIT);

        // when
        couponService.resetCouponIssued(TEST_COUPON_ID);

        // then
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(0);
        assertThat(couponService.isAlreadyIssued(TEST_COUPON_ID, 1L)).isFalse();
    }

    @Test
    @DisplayName("DB 정합성 동기화가 정상 동작한다")
    void syncWithDatabase() {
        // given
        int dbCount = 50;

        // when
        couponService.syncWithDatabase(TEST_COUPON_ID, dbCount);

        // then
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(50);
    }

    @Test
    @DisplayName("100명이 동시에 요청 시 정확히 100명만 발급받는다 (동시성 테스트)")
    void tryIssueCoupon_Concurrency_100() throws InterruptedException {
        // given
        int threadCount = 150; // 150명이 동시 요청
        int limit = 100;       // 100명만 발급 가능
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 150명이 동시에 쿠폰 발급 요청
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, userId, limit);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (IllegalStateException e) {
                    // 중복 발급은 발생하지 않아야 함 (각 userId가 다르므로)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(100); // 정확히 100명만 성공
        assertThat(failCount.get()).isEqualTo(50);     // 나머지 50명 실패
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 환경에서 중복 발급이 방지된다")
    void tryIssueCoupon_Concurrency_NoDuplicate() throws InterruptedException {
        // given
        int threadCount = 10; // 같은 사용자가 10번 동시 요청
        Long userId = 1L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 10번 동시 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, userId, TOTAL_LIMIT);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (IllegalStateException e) {
                    // 중복 발급 예외
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 1번만 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("한도가 1일 때 동시 요청 시 정확히 1명만 발급받는다")
    void tryIssueCoupon_Concurrency_Limit1() throws InterruptedException {
        // given
        int threadCount = 100;
        int limit = 1; // 단 1명만!
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    boolean result = couponService.tryIssueCoupon(TEST_COUPON_ID, userId, limit);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 1명만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(couponService.getIssuedCount(TEST_COUPON_ID)).isEqualTo(1);
    }
}
