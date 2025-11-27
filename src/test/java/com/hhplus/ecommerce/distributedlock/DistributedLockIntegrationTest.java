package com.hhplus.ecommerce.distributedlock;

import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.domain.order.Order;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 분산락 통합 테스트
 *
 * 테스트 목적:
 * - Redis 분산락이 멀티 스레드 환경에서 올바르게 동작하는지 검증
 * - 동일 사용자의 동시 주문이 순차 처리되는지 확인
 * - 다른 사용자는 병렬 처리되는지 확인
 *
 * Redis 서버:
 * - application.yml에 설정된 Redis 서버 사용 (192.168.4.81:6379)
 * - Docker Redis 서버 필요 (사용자가 미리 실행)
 *
 * 주의사항:
 * - Redis 서버가 실행 중이어야 함
 * - 테스트 실행 전 Redis 데이터 초기화 권장
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DistributedLockIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private ProductOption productOption;

    /**
     * 테스트 데이터 셋업
     * - 각 테스트 실행 전 호출
     * - 상품, 상품 옵션 생성 (재고 1000개)
     */
    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        orderRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();

        // 테스트용 상품 생성
        Product product = Product.builder()
                .name("테스트 상품")
                .price(10000)
                .status(ProductStatus.ACTIVE)
                .build();
        Product savedProduct = productRepository.save(product);

        // 테스트용 상품 옵션 생성 (재고 1000개)
        productOption = ProductOption.builder()
                .productId(savedProduct.getId())
                .color("Black")
                .size("Free")
                .stock(1000)
                .build();
        productOption = productOptionRepository.save(productOption);
    }

    @Test
    @DisplayName("분산락 테스트 1: 동일 사용자 10개 동시 주문 → 순차 처리")
    void testDistributedLock_SameUser_SequentialProcessing() throws InterruptedException {
        // Given - 동일 사용자 ID
        Long userId = 1L;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> orderIds = new CopyOnWriteArrayList<>();

        // When - 동일 사용자가 10개 주문 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    CreateOrderCommand command = createOrderCommand(userId);
                    Order order = createOrderUseCase.execute(command);

                    successCount.incrementAndGet();
                    orderIds.add(order.getId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 모든 주문이 성공해야 함 (분산락으로 순차 처리)
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(orderIds).hasSize(10);

        // 재고 확인: 1000 - 100 = 900
        ProductOption updatedOption = productOptionRepository.findById(productOption.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(900);

        System.out.println("===== 분산락 테스트 1 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("최종 재고: " + updatedOption.getStock());
        System.out.println("==============================");
    }

    @Test
    @DisplayName("분산락 테스트 2: 다른 사용자 5명 동시 주문 → 병렬 처리 가능")
    void testDistributedLock_DifferentUsers_ParallelProcessing() throws InterruptedException {
        // Given - 5명의 다른 사용자
        int userCount = 5;
        int ordersPerUser = 2; // 각 사용자당 2개 주문
        int totalOrders = userCount * ordersPerUser;

        ExecutorService executor = Executors.newFixedThreadPool(totalOrders);
        CountDownLatch latch = new CountDownLatch(totalOrders);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> orderIds = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // When - 5명이 각각 2개씩 주문 (총 10개 주문)
        for (int userId = 1; userId <= userCount; userId++) {
            final long finalUserId = userId;
            for (int j = 0; j < ordersPerUser; j++) {
                executor.submit(() -> {
                    try {
                        CreateOrderCommand command = createOrderCommand(finalUserId);
                        Order order = createOrderUseCase.execute(command);

                        successCount.incrementAndGet();
                        orderIds.add(order.getId());
                    } catch (Exception e) {
                        System.err.println("주문 실패 (사용자 " + finalUserId + "): " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then - 모든 주문이 성공해야 함
        assertThat(successCount.get()).isEqualTo(totalOrders);
        assertThat(orderIds).hasSize(totalOrders);

        // 다른 사용자는 병렬 처리되므로 시간이 짧아야 함
        // (순차 처리라면 10초 이상, 병렬 처리라면 3초 이내)
        assertThat(duration).isLessThan(10000); // 10초 이내

        System.out.println("===== 분산락 테스트 2 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("처리 시간: " + duration + "ms");
        System.out.println("==============================");
    }

    @Test
    @DisplayName("분산락 테스트 3: 재고 부족 시에도 분산락 정상 동작")
    void testDistributedLock_StockShortage() throws InterruptedException {
        // Given - 재고가 50개만 있는 상품 옵션으로 변경
        productOption.decreaseStock(950); // 1000 - 950 = 50
        productOptionRepository.save(productOption);

        Long userId = 100L;
        int threadCount = 10; // 10개 주문 시도 (각 10개씩 = 총 100개 필요)

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 재고보다 많은 주문 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    CreateOrderCommand command = createOrderCommand(userId);
                    createOrderUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 5개 성공, 5개 실패 (재고 50개 / 10개씩 = 5개 주문 가능)
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // 재고 확인: 0개 남음
        ProductOption updatedOption = productOptionRepository.findById(productOption.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(0);

        System.out.println("===== 분산락 테스트 3 결과 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건 (재고 부족)");
        System.out.println("최종 재고: " + updatedOption.getStock());
        System.out.println("==============================");
    }

    /**
     * 테스트용 주문 생성 Command 생성
     * - 상품 10개씩 주문
     */
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
