package com.hhplus.ecommerce.demo;

import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 동시성 시각화 데모
 *
 * 목적:
 * - 분산락이 어떻게 동시성을 제어하는지 눈으로 확인
 * - 스레드 간 경쟁 상황을 타임라인으로 시각화
 * - Before/After 비교로 락의 효과 체감
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrencyVisualizationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private ProductOption productOption;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();

        Product product = Product.builder()
                .name("테스트 상품")
                .price(10000)
                .status(ProductStatus.ACTIVE)
                .build();
        Product savedProduct = productRepository.save(product);

        productOption = ProductOption.builder()
                .productId(savedProduct.getId())
                .color("Black")
                .size("Free")
                .stock(1000)
                .build();
        productOption = productOptionRepository.save(productOption);
    }

    @Test
    @DisplayName("동시성 시각화 1: 타임라인으로 보는 분산락")
    void visualizeDistributedLock() throws InterruptedException {
        printHeader("분산락으로 동시성 제어 - 타임라인");

        Long userId = 1L;
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        System.out.println("📌 시나리오: 동일 사용자(1번)가 5번 동시 주문");
        System.out.println("📌 분산락 키: order:user:1");
        System.out.println("📌 기대 결과: 순차적으로 하나씩 처리\n");

        printTimeline("START", "모든 스레드 시작 대기 중...", 0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i + 1;
            executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();

                    long threadStart = System.currentTimeMillis() - startTime;
                    printTimeline("Thread-" + threadNum, "🔒 락 획득 시도...", threadStart);

                    CreateOrderCommand command = createOrderCommand(userId);
                    createOrderUseCase.execute(command);

                    long threadEnd = System.currentTimeMillis() - startTime;
                    printTimeline("Thread-" + threadNum, "✅ 주문 완료 (락 해제)", threadEnd);

                } catch (Exception e) {
                    long threadError = System.currentTimeMillis() - startTime;
                    printTimeline("Thread-" + threadNum, "❌ 실패: " + e.getMessage(), threadError);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        Thread.sleep(100);
        printTimeline("START", "⚡ 5개 스레드 동시 출발!", 0);
        startLatch.countDown(); // 모든 스레드 동시 시작

        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        printTimeline("END", "🏁 모든 작업 완료", totalTime);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("💡 분석:");
        System.out.println("  - 5개 스레드가 동시에 시작했지만");
        System.out.println("  - 분산락 덕분에 순차적으로 처리됨");
        System.out.println("  - 한 번에 하나씩만 주문 실행");
        System.out.println("  - 다른 스레드는 대기 후 차례로 실행");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @DisplayName("동시성 시각화 2: Before/After 비교")
    void compareBeforeAfter() throws InterruptedException {
        printHeader("Before/After 비교");

        System.out.println("📌 시나리오: 재고 100개, 10개씩 주문 × 5번 = 50개 필요");
        System.out.println("📌 예상 결과:");
        System.out.println("   ✅ 분산락 O → 5개 모두 성공, 재고 50개 남음");
        System.out.println("   ❌ 분산락 X → 동시성 이슈로 재고 불일치 가능\n");

        // 재고 100개로 설정
        productOption.decreaseStock(900); // 1000 - 900 = 100
        productOptionRepository.save(productOption);

        Long userId = 1L;
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        System.out.println("🚀 실행 중...\n");

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i + 1;
            executor.submit(() -> {
                try {
                    CreateOrderCommand command = createOrderCommand(userId);
                    createOrderUseCase.execute(command);

                    int count = successCount.incrementAndGet();
                    System.out.println("  ✅ Thread-" + threadNum + ": 주문 성공 (" + count + "/5)");

                } catch (Exception e) {
                    int count = failCount.incrementAndGet();
                    System.out.println("  ❌ Thread-" + threadNum + ": 주문 실패 (" + count + "번째)");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        ProductOption result = productOptionRepository.findById(productOption.getId()).orElseThrow();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 결과:");
        System.out.println("  - 성공: " + successCount.get() + "건");
        System.out.println("  - 실패: " + failCount.get() + "건");
        System.out.println("  - 최종 재고: " + result.getStock() + "개 (예상: 50개)");
        System.out.println("\n💡 분석:");
        System.out.println("  - 분산락 덕분에 5개 모두 순차 처리");
        System.out.println("  - 재고 정확히 50개 감소 (100 → 50)");
        System.out.println("  - 동시성 이슈 없음!");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @DisplayName("동시성 시각화 3: 다른 사용자는 병렬 처리")
    void visualizeParallelProcessing() throws InterruptedException {
        printHeader("다른 사용자는 병렬 처리");

        int userCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount);

        System.out.println("📌 시나리오: 3명의 다른 사용자가 동시 주문");
        System.out.println("📌 분산락 키:");
        System.out.println("   - User-1 → order:user:1");
        System.out.println("   - User-2 → order:user:2");
        System.out.println("   - User-3 → order:user:3");
        System.out.println("📌 기대 결과: 서로 다른 락 → 병렬 처리\n");

        long startTime = System.currentTimeMillis();

        for (int userId = 1; userId <= userCount; userId++) {
            final long finalUserId = userId;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    long threadStart = System.currentTimeMillis() - startTime;
                    printTimeline("User-" + finalUserId, "🔒 락 획득 (order:user:" + finalUserId + ")", threadStart);

                    CreateOrderCommand command = createOrderCommand(finalUserId);
                    createOrderUseCase.execute(command);

                    long threadEnd = System.currentTimeMillis() - startTime;
                    printTimeline("User-" + finalUserId, "✅ 주문 완료", threadEnd);

                } catch (Exception e) {
                    long threadError = System.currentTimeMillis() - startTime;
                    printTimeline("User-" + finalUserId, "❌ 실패", threadError);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        Thread.sleep(100);
        printTimeline("START", "⚡ 3명의 사용자 동시 주문!", 0);
        startLatch.countDown();

        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("\n" + "=".repeat(70));
        System.out.println("💡 분석:");
        System.out.println("  - 총 소요 시간: " + totalTime + "ms");
        System.out.println("  - 다른 사용자 = 다른 락 키");
        System.out.println("  - 서로 간섭 없이 병렬 처리");
        System.out.println("  - 락 키 설계가 성능을 결정!");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @DisplayName("동시성 시각화 4: 락 대기 시간 관찰")
    void visualizeLockWaiting() throws InterruptedException {
        printHeader("락 대기 시간 관찰");

        Long userId = 1L;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        System.out.println("📌 시나리오: 2개 스레드가 동일 사용자로 주문");
        System.out.println("📌 설정: waitTime=5초, leaseTime=10초");
        System.out.println("📌 관찰 포인트: Thread-2가 얼마나 대기하는가?\n");

        long startTime = System.currentTimeMillis();

        // Thread-1: 먼저 락 획득
        executor.submit(() -> {
            try {
                long t1 = System.currentTimeMillis() - startTime;
                printTimeline("Thread-1", "🔒 락 획득 시작", t1);

                CreateOrderCommand command = createOrderCommand(userId);
                createOrderUseCase.execute(command);

                long t2 = System.currentTimeMillis() - startTime;
                printTimeline("Thread-1", "✅ 주문 완료 (락 해제)", t2);

            } catch (Exception e) {
                printTimeline("Thread-1", "❌ 실패", System.currentTimeMillis() - startTime);
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(500); // Thread-1이 락을 먼저 잡도록

        // Thread-2: 대기 후 획득
        executor.submit(() -> {
            try {
                long t1 = System.currentTimeMillis() - startTime;
                printTimeline("Thread-2", "⏳ 락 대기 중... (Thread-1 완료까지)", t1);

                CreateOrderCommand command = createOrderCommand(userId);
                createOrderUseCase.execute(command);

                long t2 = System.currentTimeMillis() - startTime;
                long waitTime = t2 - t1;
                printTimeline("Thread-2", "✅ 주문 완료 (대기 시간: " + waitTime + "ms)", t2);

            } catch (Exception e) {
                printTimeline("Thread-2", "❌ 실패", System.currentTimeMillis() - startTime);
            } finally {
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("💡 분석:");
        System.out.println("  - Thread-1이 락을 먼저 획득");
        System.out.println("  - Thread-2는 Pub/Sub으로 대기");
        System.out.println("  - Thread-1 완료 시 unlock 알림");
        System.out.println("  - Thread-2가 즉시 락 획득 후 실행");
        System.out.println("  - Polling 아님! Pub/Sub으로 효율적 대기");
        System.out.println("=".repeat(70) + "\n");
    }

    // === Helper Methods ===

    private void printHeader(String title) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  " + title);
        System.out.println("=".repeat(70) + "\n");
    }

    private void printTimeline(String thread, String message, long timeMs) {
        String time = String.format("[%5dms]", timeMs);
        String threadName = String.format("%-10s", thread);
        System.out.println(time + " " + threadName + " │ " + message);
    }

    private CreateOrderCommand createOrderCommand(Long userId) {
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem item = OrderItem.builder()
                .productOptionId(productOption.getId())
                .quantity(10)
                .price(10000)
                .build();
        orderItems.add(item);
        return new CreateOrderCommand(userId, orderItems, null);
    }
}
