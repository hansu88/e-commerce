package com.hhplus.ecommerce.concurrency;

import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * 재고 차감 동시성 테스트
 * - 여러 주문이 동시에 발생할 때 재고가 정확히 차감되는지 검증
 * - 재고 부족 시 예외가 발생하는지 검증
 * - 재고가 절대 음수가 되지 않는지 검증
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StockConcurrencyTest {

    @Autowired
    private DecreaseStockUseCase decreaseStockUseCase;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    private void runConcurrentDecrease(ProductOption option, int threadCount, int[] quantities,
                                       AtomicInteger successCount, AtomicInteger failCount) throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(threadCount, 50));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int quantity = quantities[i];
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    DecreaseStockCommand command = new DecreaseStockCommand(option.getId(), quantity, StockChangeReason.ORDER);
                    decreaseStockUseCase.execute(command);
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
    @DisplayName("동시성 테스트 1: 재고 100개, 동시 요청 100개(각 1개씩)")
    void concurrentStockDecrease_ExactLimit() throws InterruptedException {
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Black");
        option.setSize("270");
        option.setStock(100);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 100;
        int[] quantities = new int[threadCount];
        for (int i = 0; i < threadCount; i++) quantities[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentDecrease(savedOption, threadCount, quantities, successCount, failCount);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(0),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0)
        );
    }

    @Test
    @DisplayName("동시성 테스트 2: 재고 100개, 동시 요청 150개 - 100개 성공, 50개 실패")
    void concurrentStockDecrease_ExceedLimit() throws InterruptedException {
        ProductOption option = new ProductOption();
        option.setProductId(2L);
        option.setColor("White");
        option.setSize("260");
        option.setStock(100);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 150;
        int[] quantities = new int[threadCount];
        for (int i = 0; i < threadCount; i++) quantities[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentDecrease(savedOption, threadCount, quantities, successCount, failCount);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(100),
                () -> assertThat(failCount.get()).isEqualTo(50),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0)
        );
    }

    @Test
    @DisplayName("동시성 테스트 3: 재고 10개, 동시 요청 20개 - 재고 절대 음수 안됨")
    void concurrentStockDecrease_NeverNegative() throws InterruptedException {
        ProductOption option = new ProductOption();
        option.setProductId(3L);
        option.setColor("Red");
        option.setSize("265");
        option.setStock(10);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 20;
        int[] quantities = new int[threadCount];
        for (int i = 0; i < threadCount; i++) quantities[i] = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentDecrease(savedOption, threadCount, quantities, successCount, failCount);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(failCount.get()).isEqualTo(10),
                () -> assertThat(updatedOption.getStock()).isEqualTo(0),
                () -> assertThat(updatedOption.getStock()).isGreaterThanOrEqualTo(0)
        );
    }

    @Test
    @DisplayName("동시성 테스트 4: 재고 50개, 다양한 수량 동시 요청")
    void concurrentStockDecrease_MultipleQuantities() throws InterruptedException {
        ProductOption option = new ProductOption();
        option.setProductId(4L);
        option.setColor("Blue");
        option.setSize("275");
        option.setStock(50);
        ProductOption savedOption = productOptionRepository.save(option);

        int threadCount = 20;
        int[] quantities = new int[threadCount];
        for (int i = 0; i < threadCount; i++) quantities[i] = (i % 5) + 1; // 1~5 순환

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        runConcurrentDecrease(savedOption, threadCount, quantities, successCount, failCount);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        int totalDecreasedQty = 0;
        for (int q : quantities) totalDecreasedQty += q;
        int expectedStock = Math.max(50 - totalDecreasedQty, 0);

        assertAll(
                () -> assertThat(updatedOption.getStock()).isEqualTo(expectedStock),
                () -> assertThat(updatedOption.getStock()).isGreaterThanOrEqualTo(0)
        );
    }
}
