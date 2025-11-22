package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.PopularProduct;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AggregatePopularProductsUseCaseTest {

    @Autowired
    private AggregatePopularProductsUseCase aggregatePopularProductsUseCase;

    @Autowired
    private PopularProductRepository popularProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();
        popularProductRepository.deleteAll();
    }

    @Test
    @DisplayName("일별 인기 상품 집계 - 정상 동작")
    void aggregateDaily_Success() {
        // Given
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime targetDateTime = targetDate.atStartOfDay().plusHours(12);

        // 상품 A
        Product productA = createProduct("상품 A", 10000);
        ProductOption optionA = createProductOption(productA.getId(), "Red", "M", 100);

        // 상품 B
        Product productB = createProduct("상품 B", 20000);
        ProductOption optionB = createProductOption(productB.getId(), "Blue", "L", 100);

        // 주문 생성 (상품 A: 5개, 상품 B: 10개)
        createOrderItem(optionA.getId(), 5, targetDateTime);
        createOrderItem(optionB.getId(), 10, targetDateTime);

        // When
        aggregatePopularProductsUseCase.aggregateDaily(targetDate);

        // Then
        List<PopularProduct> results = popularProductRepository.findByPeriodTypeAndAggregatedDate(
                PopularProduct.PeriodType.DAILY,
                targetDate
        );

        assertThat(results).hasSize(2);

        // 판매량 순 정렬 확인 (상품 B가 1위)
        assertThat(results.get(0).getProductId()).isEqualTo(productB.getId());
        assertThat(results.get(0).getSalesCount()).isEqualTo(10);

        assertThat(results.get(1).getProductId()).isEqualTo(productA.getId());
        assertThat(results.get(1).getSalesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("일별 인기 상품 집계 - 동일 상품 여러 옵션 합산")
    void aggregateDaily_MultipleOptions() {
        // Given
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime targetDateTime = targetDate.atStartOfDay().plusHours(12);

        Product product = createProduct("상품 A", 10000);
        ProductOption option1 = createProductOption(product.getId(), "Red", "M", 100);
        ProductOption option2 = createProductOption(product.getId(), "Blue", "L", 100);

        // 같은 상품의 다른 옵션 판매
        createOrderItem(option1.getId(), 5, targetDateTime);
        createOrderItem(option2.getId(), 3, targetDateTime);

        // When
        aggregatePopularProductsUseCase.aggregateDaily(targetDate);

        // Then
        List<PopularProduct> results = popularProductRepository.findByPeriodTypeAndAggregatedDate(
                PopularProduct.PeriodType.DAILY,
                targetDate
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo(product.getId());
        assertThat(results.get(0).getSalesCount()).isEqualTo(8); // 5 + 3
    }

    @Test
    @DisplayName("월별 인기 상품 집계 - 정상 동작")
    void aggregateMonthly_Success() {
        // Given
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        LocalDate firstDay = targetMonth.atDay(1);
        LocalDateTime targetDateTime = firstDay.atStartOfDay().plusDays(15);

        Product productA = createProduct("상품 A", 10000);
        ProductOption optionA = createProductOption(productA.getId(), "Red", "M", 100);

        Product productB = createProduct("상품 B", 20000);
        ProductOption optionB = createProductOption(productB.getId(), "Blue", "L", 100);

        // 전월 주문 생성
        createOrderItem(optionA.getId(), 30, targetDateTime);
        createOrderItem(optionB.getId(), 50, targetDateTime);

        // When
        aggregatePopularProductsUseCase.aggregateMonthly(targetMonth);

        // Then
        List<PopularProduct> results = popularProductRepository.findByPeriodTypeAndAggregatedDate(
                PopularProduct.PeriodType.MONTHLY,
                firstDay
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProductId()).isEqualTo(productB.getId());
        assertThat(results.get(0).getSalesCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("집계 데이터 업데이트 - 중복 집계 시 기존 데이터 업데이트")
    void aggregateDaily_Update() {
        // Given
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime targetDateTime = targetDate.atStartOfDay().plusHours(12);

        Product product = createProduct("상품 A", 10000);
        ProductOption option = createProductOption(product.getId(), "Red", "M", 100);

        createOrderItem(option.getId(), 5, targetDateTime);

        // 첫 번째 집계
        aggregatePopularProductsUseCase.aggregateDaily(targetDate);

        List<PopularProduct> firstResults = popularProductRepository.findByPeriodTypeAndAggregatedDate(
                PopularProduct.PeriodType.DAILY,
                targetDate
        );
        assertThat(firstResults).hasSize(1);
        assertThat(firstResults.get(0).getSalesCount()).isEqualTo(5);

        // 추가 주문
        createOrderItem(option.getId(), 3, targetDateTime);

        // When - 재집계
        aggregatePopularProductsUseCase.aggregateDaily(targetDate);

        // Then - 업데이트 확인
        List<PopularProduct> secondResults = popularProductRepository.findByPeriodTypeAndAggregatedDate(
                PopularProduct.PeriodType.DAILY,
                targetDate
        );

        assertThat(secondResults).hasSize(1);
        assertThat(secondResults.get(0).getSalesCount()).isEqualTo(8); // 5 + 3
        assertThat(secondResults.get(0).getId()).isEqualTo(firstResults.get(0).getId()); // 같은 레코드
    }

    // === Helper Methods ===

    private Product createProduct(String name, int price) {
        Product product = new Product(name, price, ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    private ProductOption createProductOption(Long productId, String color, String size, int stock) {
        ProductOption option = ProductOption.builder()
                .productId(productId)
                .color(color)
                .size(size)
                .stock(stock)
                .build();
        return productOptionRepository.save(option);
    }

    private void createOrderItem(Long productOptionId, int quantity, LocalDateTime createdAt) {
        Order order = Order.builder()
                .userId(1L)
                .status(OrderStatus.CREATED)
                .totalAmount(10000)
                .discountAmount(0)
                .build();
        order = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .orderId(order.getId())
                .productOptionId(productOptionId)
                .quantity(quantity)
                .price(10000)
                .createdAt(createdAt)
                .build();
        orderItemRepository.save(orderItem);
    }
}
