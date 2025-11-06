package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.service.CouponService;
import com.hhplus.ecommerce.application.service.OrderService;
import com.hhplus.ecommerce.application.service.StockService;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.*;
import com.hhplus.ecommerce.infrastructure.persistence.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 통합 테스트: 전체 주문 플로우 테스트
 * - 상품 조회 -> 주문 생성 -> 재고 차감 -> 결제 -> 취소 -> 재고 복구
 */
class OrderIntegrationTest {

    private OrderService orderService;
    private StockService stockService;
    private CouponService couponService;
    private ProductRepository productRepository;
    private ProductOptionRepository productOptionRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private CouponRepository couponRepository;
    private UserCouponRepository userCouponRepository;
    private StockHistoryRepository stockHistoryRepository;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productOptionRepository = new InMemoryProductOptionRepository();
        orderRepository = new InMemoryOrderRepository();
        orderItemRepository = new InMemoryOrderItemRepository();
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        stockHistoryRepository = new InMemoryStockHistoryRepository();

        // Service 초기화
        stockService = new StockService(productOptionRepository, stockHistoryRepository);
        couponService = new CouponService(couponRepository, userCouponRepository);
        orderService = new OrderService(orderRepository, orderItemRepository, stockService, couponService);
    }

    @Test
    @DisplayName("통합 테스트: 주문 생성 -> 결제 -> 재고 차감 확인")
    void orderCreateAndPayFlow() {
        // Given - 상품 및 옵션 생성
        Product product = new Product();
        product.setName("나이키 에어맥스");
        product.setPrice(150000);
        Product savedProduct = productRepository.save(product);

        ProductOption option = new ProductOption();
        option.setProductId(savedProduct.getId());
        option.setColor("Black");
        option.setSize("270");
        option.setStock(10);
        ProductOption savedOption = productOptionRepository.save(option);

        // When - 주문 생성 (5개 주문)
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(savedOption.getId());
        orderItem.setQuantity(5);
        orderItem.setPrice(savedProduct.getPrice());
        orderItems.add(orderItem);

        Order order = orderService.createOrder(1L, orderItems, null);

        // Then - 주문 생성 및 재고 차감 확인 (10 -> 5)
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(750000); // 150000 * 5

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(5);

        // When - 결제 처리
        Order paidOrder = orderService.payOrder(order.getId(), "CARD");

        // Then - 결제 완료 확인
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("통합 테스트: 주문 취소 -> 재고 복구 확인")
    void orderCancelAndStockRestoreFlow() {
        // Given - 상품 및 옵션 생성 ,  7개 주문
        Product product = new Product();
        product.setName("아디다스 울트라부스트");
        product.setPrice(200000);
        Product savedProduct = productRepository.save(product);

        ProductOption option = new ProductOption();
        option.setProductId(savedProduct.getId());
        option.setColor("White");
        option.setSize("260");
        option.setStock(20);
        ProductOption savedOption = productOptionRepository.save(option);
        
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(savedOption.getId());
        orderItem.setQuantity(7);
        orderItem.setPrice(savedProduct.getPrice());
        orderItems.add(orderItem);

        Order order = orderService.createOrder(1L, orderItems, null);

        // 재고 차감 확인 (20 -> 13)
        ProductOption afterOrder = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(afterOrder.getStock()).isEqualTo(13);

        // When - 주문 취소
        Order cancelledOrder = orderService.cancelOrder(order.getId());

        // Then - 취소 상태 확인
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Then - 재고 복구 확인 (13 -> 20)
        ProductOption afterCancel = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(afterCancel.getStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("통합 테스트: 쿠폰 발급 -> 주문 시 사용 -> 취소 시 복구")
    void orderWithCouponFlow() {
        // Given - 상품 및 옵션 생성
        Product product = new Product();
        product.setName("푸마 스니커즈");
        product.setPrice(100000);
        Product savedProduct = productRepository.save(product);

        ProductOption option = new ProductOption();
        option.setProductId(savedProduct.getId());
        option.setColor("Red");
        option.setSize("265");
        option.setStock(50);
        ProductOption savedOption = productOptionRepository.save(option);

        // Given - 쿠폰 생성
        Coupon coupon = new Coupon();
        coupon.setCode("NEWUSER");
        coupon.setDiscountAmount(10000);
        coupon.setTotalQuantity(100);
        coupon.setIssuedQuantity(0);
        coupon.setValidUntil(LocalDateTime.now().plusDays(30));
        Coupon savedCoupon = couponRepository.save(coupon);

        // When - 쿠폰 발급
        UserCoupon userCoupon = couponService.issueCoupon(1L, savedCoupon.getId());
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);

        // When - 주문 생성 (쿠폰 사용)
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(savedOption.getId());
        orderItem.setQuantity(3);
        orderItem.setPrice(savedProduct.getPrice());
        orderItems.add(orderItem);

        Order order = orderService.createOrder(1L, orderItems, userCoupon.getId());

        // Then - 주문 생성 및 쿠폰 사용 확인
        assertThat(order.getUserCouponId()).isEqualTo(userCoupon.getId());

        // Then - 할인 금액 적용 확인
        int expectedSubtotal = 100000 * 3; // 300,000원
        int expectedDiscount = 10000;      // 10,000원 할인
        int expectedFinalAmount = 290000;  // 290,000원

        assertThat(order.getDiscountAmount()).isEqualTo(expectedDiscount);
        assertThat(order.getTotalAmount()).isEqualTo(expectedFinalAmount);

        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(usedCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);

        // When - 주문 취소
        Order cancelledOrder = orderService.cancelOrder(order.getId());

        // Then - 쿠폰 복구 확인
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        UserCoupon restoredCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(restoredCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(restoredCoupon.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("통합 테스트: 여러 상품 주문 -> 부분 취소 불가 (전체 취소만 가능)")
    void multipleProductsOrderFlow() {
        // Given - 상품 2개 생성
        Product product1 = new Product();
        product1.setName("상품 A");
        product1.setPrice(50000);
        Product savedProduct1 = productRepository.save(product1);

        ProductOption option1 = new ProductOption();
        option1.setProductId(savedProduct1.getId());
        option1.setColor("Blue");
        option1.setSize("M");
        option1.setStock(30);
        ProductOption savedOption1 = productOptionRepository.save(option1);

        Product product2 = new Product();
        product2.setName("상품 B");
        product2.setPrice(80000);
        Product savedProduct2 = productRepository.save(product2);

        ProductOption option2 = new ProductOption();
        option2.setProductId(savedProduct2.getId());
        option2.setColor("Green");
        option2.setSize("L");
        option2.setStock(25);
        ProductOption savedOption2 = productOptionRepository.save(option2);

        // When - 여러 상품 주문
        List<OrderItem> orderItems = new ArrayList<>();

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setProductOptionId(savedOption1.getId());
        orderItem1.setQuantity(5);
        orderItem1.setPrice(savedProduct1.getPrice());
        orderItems.add(orderItem1);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProductOptionId(savedOption2.getId());
        orderItem2.setQuantity(3);
        orderItem2.setPrice(savedProduct2.getPrice());
        orderItems.add(orderItem2);

        Order order = orderService.createOrder(1L, orderItems, null);

        // Then - 재고 차감 확인
        ProductOption afterOption1 = productOptionRepository.findById(savedOption1.getId()).orElseThrow();
        assertThat(afterOption1.getStock()).isEqualTo(25); // 30 - 5

        ProductOption afterOption2 = productOptionRepository.findById(savedOption2.getId()).orElseThrow();
        assertThat(afterOption2.getStock()).isEqualTo(22); // 25 - 3

        // Then - 총 금액 확인
        int expectedTotal = (50000 * 5) + (80000 * 3); // 250000 + 240000 = 490000
        assertThat(order.getTotalAmount()).isEqualTo(expectedTotal);

        // When - 주문 취소 (전체 취소)
        Order cancelledOrder = orderService.cancelOrder(order.getId());

        // Then - 모든 재고 복구 확인
        ProductOption restoredOption1 = productOptionRepository.findById(savedOption1.getId()).orElseThrow();
        assertThat(restoredOption1.getStock()).isEqualTo(30); // 25 + 5 = 원상복구

        ProductOption restoredOption2 = productOptionRepository.findById(savedOption2.getId()).orElseThrow();
        assertThat(restoredOption2.getStock()).isEqualTo(25); // 22 + 3 = 원상복구
    }
}
