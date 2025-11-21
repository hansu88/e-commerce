package com.hhplus.ecommerce.performance;

import com.hhplus.ecommerce.application.command.cart.AddCartItemCommand;
import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.cart.AddCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.product.AggregatePopularProductsUseCase;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 병목 지점 성능 테스트
 * - STEP 1~4에서 개선한 내용 검증
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PerformanceTest {

    @Autowired
    private AggregatePopularProductsUseCase aggregatePopularProductsUseCase;

    @Autowired
    private AddCartItemUseCase addCartItemUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 성능 테스트 1: 인기 상품 집계 (Native SQL)
     * - STEP 2에서 개선: 2,741 쿼리 → 1 쿼리
     * - 목표: 100ms 이내
     */
    @Test
    @DisplayName("성능 테스트: 인기 상품 집계 - Native SQL 효과 검증")
    void performance_aggregate_popular_products() {
        // Given: 상품 10개, 주문 항목 1,000개 생성
        List<Product> products = new ArrayList<>();
        List<ProductOption> options = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Product product = new Product("상품" + i, 10000 * i, ProductStatus.ACTIVE);
            products.add(productRepository.save(product));

            ProductOption option = new ProductOption();
            option.setProductId(product.getId());
            option.setColor("RED");
            option.setSize("L");
            option.setStock(1000);
            options.add(productOptionRepository.save(option));
        }

        // 1,000개 주문 항목 생성
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 1000; i++) {
            OrderItem item = new OrderItem();
            item.setOrderId((long) (i + 1));
            item.setProductOptionId(options.get(i % 10).getId());
            item.setQuantity(1);
            item.setPrice(10000);
            item.setCreatedAt(now);
            orderItemRepository.save(item);
        }

        // When: 인기 상품 집계 실행 (성능 측정)
        long startTime = System.currentTimeMillis();
        aggregatePopularProductsUseCase.aggregateDaily(LocalDate.now());
        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 100ms 이내
        System.out.println("인기 상품 집계 실행 시간: " + elapsed + "ms");
        assertThat(elapsed).isLessThan(100);
    }

    /**
     * 성능 테스트 2: 장바구니 동시 추가
     * - STEP 3에서 개선: UNIQUE 제약조건으로 중복 방지
     * - 목표: 중복 행 생성 0건
     */
    @Test
    @DisplayName("성능 테스트: 장바구니 동시 추가 - 중복 방지 검증")
    void performance_cart_concurrent_add() throws InterruptedException {
        // Given: 상품 옵션 1개
        Product product = new Product("테스트 상품", 10000, ProductStatus.ACTIVE);
        productRepository.save(product);

        ProductOption option = new ProductOption();
        option.setProductId(product.getId());
        option.setColor("BLACK");
        option.setSize("M");
        option.setStock(1000);
        ProductOption savedOption = productOptionRepository.save(option);

        Long userId = 1L;
        int threadCount = 50;

        // When: 50번 동시에 같은 상품 추가
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    AddCartItemCommand command = new AddCartItemCommand(
                        userId,
                        savedOption.getId(),
                        1
                    );
                    addCartItemUseCase.execute(command);
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: 장바구니에 1개 항목만 존재, 수량 50
        List<CartItem> cartItems = cartItemRepository.findAll();

        assertAll(
            () -> assertThat(cartItems).hasSize(1),
            () -> assertThat(cartItems.get(0).getQuantity()).isEqualTo(50),
            () -> assertThat(cartItems.get(0).getProductOptionId()).isEqualTo(savedOption.getId())
        );
    }

    /**
     * 성능 테스트 3: 대량 주문 생성 (Batch Insert)
     * - STEP 4에서 개선: N번 INSERT → 1번 Batch INSERT
     * - 목표: 10개 상품 주문 생성 50ms 이내
     */
    @Test
    @DisplayName("성능 테스트: 대량 주문 생성 - Batch Insert 효과 검증")
    void performance_bulk_order_creation() {
        // Given: 10개 상품 옵션 생성
        List<ProductOption> options = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product product = new Product("상품" + i, 10000, ProductStatus.ACTIVE);
            productRepository.save(product);

            ProductOption option = new ProductOption();
            option.setProductId(product.getId());
            option.setColor("BLUE");
            option.setSize("L");
            option.setStock(1000);
            options.add(productOptionRepository.save(option));
        }

        // 10개 상품 주문 준비
        List<OrderItem> orderItems = new ArrayList<>();
        for (ProductOption option : options) {
            OrderItem item = new OrderItem();
            item.setProductOptionId(option.getId());
            item.setQuantity(1);
            item.setPrice(10000);
            orderItems.add(item);
        }

        // When: 주문 생성 (성능 측정)
        long startTime = System.currentTimeMillis();
        CreateOrderCommand command = new CreateOrderCommand(1L, orderItems, null);
        Order order = createOrderUseCase.execute(command);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 50ms 이내, 주문 항목 10개 생성
        System.out.println("10개 상품 주문 생성 시간: " + elapsed + "ms");

        assertAll(
            () -> assertThat(elapsed).isLessThan(50),
            () -> assertThat(order.getId()).isNotNull(),
            () -> assertThat(orderItemRepository.findByOrderId(order.getId())).hasSize(10)
        );
    }

    /**
     * 성능 테스트 4: 대량 주문 동시 생성
     * - 동시성 + Batch Insert 효과 검증
     * - 목표: 100개 주문 동시 생성 3초 이내
     */
    @Test
    @DisplayName("성능 테스트: 대량 주문 동시 생성 - 동시성 + Batch 검증")
    void performance_concurrent_order_creation() throws InterruptedException {
        // Given: 상품 옵션 5개 생성
        List<ProductOption> options = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Product product = new Product("상품" + i, 10000, ProductStatus.ACTIVE);
            productRepository.save(product);

            ProductOption option = new ProductOption();
            option.setProductId(product.getId());
            option.setColor("WHITE");
            option.setSize("XL");
            option.setStock(10000);
            options.add(productOptionRepository.save(option));
        }

        int orderCount = 100;
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 100개 주문 동시 생성
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(orderCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < orderCount; i++) {
            final long userId = (long) (i + 1);
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // 3개 상품 주문
                    List<OrderItem> items = new ArrayList<>();
                    for (int j = 0; j < 3; j++) {
                        OrderItem item = new OrderItem();
                        item.setProductOptionId(options.get(j).getId());
                        item.setQuantity(1);
                        item.setPrice(10000);
                        items.add(item);
                    }

                    CreateOrderCommand command = new CreateOrderCommand(userId, items, null);
                    createOrderUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족 등으로 실패 가능
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 3초 이내, 성공한 주문 수 확인
        System.out.println("100개 주문 동시 생성 시간: " + elapsed + "ms");
        System.out.println("성공한 주문 수: " + successCount.get());

        assertAll(
            () -> assertThat(elapsed).isLessThan(3000),
            () -> assertThat(successCount.get()).isGreaterThan(90) // 최소 90개 성공
        );
    }
}
