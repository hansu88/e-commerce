package com.hhplus.ecommerce.concurrency;

import com.hhplus.ecommerce.application.command.point.UsePointCommand;
import com.hhplus.ecommerce.application.usecase.point.UsePointUseCase;
import com.hhplus.ecommerce.domain.point.Point;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 포인트 사용 동시성 테스트
 * - 여러 사용자가 동시에 포인트를 사용할 때 잔액이 정확히 차감되는지 검증
 * - 포인트 부족 시 예외가 발생하는지 검증
 * - 포인트가 절대 음수가 되지 않는지 검증
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PointConcurrencyTest {

    @Autowired
    private UsePointUseCase usePointUseCase;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private Point createPoint(Long userId, Integer initialBalance) {
        Point point = new Point(userId, initialBalance);
        return pointRepository.save(point);
    }

    private void runConcurrentUse(Long userId, int threadCount, int[] amounts,
                                  AtomicInteger successCount, AtomicInteger failCount) throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(threadCount, 50));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int amount = amounts[i];
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    UsePointCommand command = new UsePointCommand(userId, amount, "동시성 테스트");
                    usePointUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시에 시작
        doneLatch.await(30, TimeUnit.SECONDS); // 모든 스레드 완료 대기
        executorService.shutdown();
    }

    @Test
    @DisplayName("동시성 테스트 1: 포인트 100, 동시 사용 100개(각 1포인트)")
    void concurrentPointUse_ExactLimit() throws InterruptedException {
        Long userId = 1L;
        Point point = createPoint(userId, 100);

        int threadCount = 100;
        int[] amounts = new int[threadCount];
        for (int i = 0; i < threadCount; i++) amounts[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentUse(userId, threadCount, amounts, successCount, failCount);

        Point updatedPoint = pointRepository.findByUserId(userId).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(updatedPoint.getBalance()).isEqualTo(0)
        );

        System.out.println("===== 포인트 동시성 테스트 1 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 잔액: " + updatedPoint.getBalance());
        System.out.println("======================================");
    }

    @Test
    @DisplayName("동시성 테스트 2: 포인트 100, 동시 사용 150개 - 100개 성공, 50개 실패")
    void concurrentPointUse_ExceedLimit() throws InterruptedException {
        Long userId = 2L;
        Point point = createPoint(userId, 100);

        int threadCount = 150;
        int[] amounts = new int[threadCount];
        for (int i = 0; i < threadCount; i++) amounts[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentUse(userId, threadCount, amounts, successCount, failCount);

        Point updatedPoint = pointRepository.findByUserId(userId).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(50),
                () -> assertThat(updatedPoint.getBalance()).isEqualTo(0)
        );

        System.out.println("===== 포인트 동시성 테스트 2 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 잔액: " + updatedPoint.getBalance());
        System.out.println("======================================");
    }

    @Test
    @DisplayName("동시성 테스트 3: 포인트 10, 동시 사용 20개 - 포인트 절대 음수 안됨")
    void concurrentPointUse_NeverNegative() throws InterruptedException {
        Long userId = 3L;
        Point point = createPoint(userId, 10);

        int threadCount = 20;
        int[] amounts = new int[threadCount];
        for (int i = 0; i < threadCount; i++) amounts[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentUse(userId, threadCount, amounts, successCount, failCount);

        Point updatedPoint = pointRepository.findByUserId(userId).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(failCount.get()).isEqualTo(10),
                () -> assertThat(updatedPoint.getBalance()).isEqualTo(0),
                () -> assertThat(updatedPoint.getBalance()).isGreaterThanOrEqualTo(0) // 절대 음수 안됨
        );

        System.out.println("===== 포인트 동시성 테스트 3 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 잔액: " + updatedPoint.getBalance());
        System.out.println("======================================");
    }

    @Test
    @DisplayName("동시성 테스트 4: 포인트 50, 다양한 금액 동시 사용")
    void concurrentPointUse_MultipleAmounts() throws InterruptedException {
        Long userId = 4L;
        Point point = createPoint(userId, 50);

        int threadCount = 20;
        int[] amounts = new int[threadCount];
        for (int i = 0; i < threadCount; i++) amounts[i] = (i % 5) + 1; // 1~5 순환

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentUse(userId, threadCount, amounts, successCount, failCount);

        Point updatedPoint = pointRepository.findByUserId(userId).orElseThrow();
        assertAll(
                () -> assertThat(updatedPoint.getBalance()).isGreaterThanOrEqualTo(0),
                () -> assertThat(updatedPoint.getBalance()).isLessThanOrEqualTo(50)
        );

        System.out.println("===== 포인트 동시성 테스트 4 결과 =====");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 잔액: " + updatedPoint.getBalance());
        System.out.println("======================================");
    }
}
