package com.hhplus.ecommerce.concurrency;

import com.hhplus.ecommerce.application.command.DecreaseStockCommand;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryStockHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 재고 차감 동시성 테스트
 * - 여러 주문이 동시에 발생할 때 재고가 정확히 차감되는지 검증
 * - 재고 부족 시 예외가 발생하는지 검증
 * - 재고가 절대 음수가 되지 않는지 검증
 */
public class StockConcurrencyTest {

    private DecreaseStockUseCase decreaseStockUseCase;
    private InMemoryProductOptionRepository productOptionRepository;
    private InMemoryStockHistoryRepository stockHistoryRepository;

    @BeforeEach
    void setUp() {
        productOptionRepository = new InMemoryProductOptionRepository();
        stockHistoryRepository = new InMemoryStockHistoryRepository();
        decreaseStockUseCase = new DecreaseStockUseCase(productOptionRepository, stockHistoryRepository);
    }

    @Test
    @DisplayName("동시성 테스트 1: 재고 100개, 동시 요청 100개(각 1개씩) - 정확히 100개 차감")
    void concurrentStockDecrease_ExactLimit() throws InterruptedException {
        // Given - 재고 100개 생성
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Black");
        option.setSize("270");
        option.setStock(100);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 100명이 동시에 각 1개씩 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    DecreaseStockCommand command = new DecreaseStockCommand(savedOption.getId(), 1, StockChangeReason.ORDER);
                    decreaseStockUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 정확히 100개 성공, 0개 실패, 재고 0
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0)
        );

        System.out.println("===== 동시성 테스트 1 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("최종 재고: " + updatedOption.getStock() + "개");
        System.out.println("=============================");
    }

    @Test
    @DisplayName("동시성 테스트 2: 재고 100개, 동시 요청 150개(각 1개씩) - 100개 성공, 50개 실패")
    void concurrentStockDecrease_ExceedLimit() throws InterruptedException {
        // Given - 재고 100개 생성
        ProductOption option = new ProductOption();
        option.setProductId(2L);
        option.setColor("White");
        option.setSize("260");
        option.setStock(100);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 150명이 동시에 각 1개씩 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    DecreaseStockCommand command = new DecreaseStockCommand(savedOption.getId(), 1, StockChangeReason.ORDER);
                    decreaseStockUseCase.execute(command);
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

        // Then - 정확히 100개 성공, 50개 실패, 재고 0
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(50),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0)
        );

        System.out.println("===== 동시성 테스트 2 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("최종 재고: " + updatedOption.getStock() + "개");
        System.out.println("=============================");
    }

    @Test
    @DisplayName("동시성 테스트 3: 재고 10개, 동시 요청 20개(각 1개씩) - 재고 절대 음수 안됨")
    void concurrentStockDecrease_NeverNegative() throws InterruptedException {
        // Given - 재고 10개 생성
        ProductOption option = new ProductOption();
        option.setProductId(3L);
        option.setColor("Red");
        option.setSize("265");
        option.setStock(10);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 20명이 동시에 각 1개씩 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    DecreaseStockCommand command = new DecreaseStockCommand(savedOption.getId(), 1, StockChangeReason.ORDER);
                    decreaseStockUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 정확히 10개 성공, 10개 실패, 재고 0 (절대 음수 안됨)
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(failCount.get()).isEqualTo(10),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0),
                () -> assertThat(updatedOption.getStock()).isGreaterThanOrEqualTo(0) // 재고는 절대 음수가 될 수 없음
        );

        System.out.println("===== 동시성 테스트 3 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("최종 재고: " + updatedOption.getStock() + "개");
        System.out.println("재고 음수 방지: " + (updatedOption.getStock() >= 0 ? "성공" : "실패"));
        System.out.println("=============================");
    }

    @Test
    @DisplayName("동시성 테스트 4: 재고 50개, 다양한 수량 동시 요청 - Race Condition 방지")
    void concurrentStockDecrease_MultipleQuantities() throws InterruptedException {
        // Given - 재고 50개 생성
        ProductOption option = new ProductOption();
        option.setProductId(4L);
        option.setColor("Blue");
        option.setSize("275");
        option.setStock(50);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalDecreasedQty = new AtomicInteger(0);

        // When - 20명이 동시에 다양한 수량(1~5개) 주문
        for (int i = 0; i < threadCount; i++) {
            final int quantity = (i % 5) + 1; // 1, 2, 3, 4, 5개씩 순환
            executorService.submit(() -> {
                try {
                    DecreaseStockCommand command = new DecreaseStockCommand(savedOption.getId(), quantity, StockChangeReason.ORDER);
                    decreaseStockUseCase.execute(command);
                    successCount.incrementAndGet();
                    totalDecreasedQty.addAndGet(quantity);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 재고가 정확히 차감되었는지 확인
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();

        // 초기 재고(50) - 차감된 수량 = 최종 재고
        assertAll(
                () -> assertThat(updatedOption.getStock()).isEqualTo(50 - totalDecreasedQty.get()),
                () -> assertThat(updatedOption.getStock()).isGreaterThanOrEqualTo(0)
        );

        System.out.println("===== 동시성 테스트 4 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("총 차감 수량: " + totalDecreasedQty.get() + "개");
        System.out.println("최종 재고: " + updatedOption.getStock() + "개");
        System.out.println("재고 정합성: " + (50 - totalDecreasedQty.get() == updatedOption.getStock() ? "성공" : "실패"));
        System.out.println("=============================");
    }
}
