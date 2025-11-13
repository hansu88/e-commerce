package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.command.order.CancelOrderCommand;
import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.command.order.PayOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.application.usecase.order.CancelOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.PayOrderUseCase;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.UserCouponStatus;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 플로우 통합 테스트
 * - 상품 조회 → 쿠폰 발급 → 주문 생성 → 결제 → 취소
 */
@SpringBootTest
@Transactional
class OrderIntegrationTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private PayOrderUseCase payOrderUseCase;

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 테스트: 상품 조회 → 쿠폰 발급 → 주문 생성 → 결제 → 취소")
    void fullOrderFlow() {
        // === 1. 상품 준비 ===
        Product product = createProduct("테스트 상품", 10000);
        ProductOption option = createProductOption(product.getId(), "Red", "M", 100);

        // === 2. 쿠폰 발급 ===
        Coupon coupon = createCoupon("DISCOUNT5000", 5000, 10, 0);
        IssueCouponCommand issueCommand = new IssueCouponCommand(1L, coupon.getId());
        UserCoupon userCoupon = issueCouponUseCase.execute(issueCommand);

        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getUserId()).isEqualTo(1L);

        // === 3. 주문 생성 ===
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(option.getId());
        orderItem.setQuantity(2);
        orderItem.setPrice(product.getPrice());
        orderItem.setCreatedAt(LocalDateTime.now());

        CreateOrderCommand createCommand = new CreateOrderCommand(
                1L,
                List.of(orderItem),
                userCoupon.getId()
        );
        Order order = createOrderUseCase.execute(createCommand);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(15000); // (10000 * 2) - 5000
        assertThat(order.getDiscountAmount()).isEqualTo(5000);

        // 재고 차감 확인
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(98); // 100 - 2

        // 쿠폰 사용 확인
        UserCoupon updatedUserCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedUserCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);

        // === 4. 결제 처리 ===
        PayOrderCommand payCommand = new PayOrderCommand(order.getId(), "CARD");
        Order paidOrder = payOrderUseCase.execute(payCommand);

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);

        // === 5. 주문 취소 ===
        CancelOrderCommand cancelCommand = new CancelOrderCommand(order.getId());
        Order cancelledOrder = cancelOrderUseCase.execute(cancelCommand);

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 재고 복구 확인
        ProductOption restoredOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(restoredOption.getStock()).isEqualTo(100); // 98 + 2

        // 쿠폰 복구 확인
        UserCoupon restoredUserCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(restoredUserCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("통합 테스트: 쿠폰 없이 주문")
    void orderWithoutCoupon() {
        // === 1. 상품 준비 ===
        Product product = createProduct("테스트 상품", 10000);
        ProductOption option = createProductOption(product.getId(), "Blue", "L", 50);

        // === 2. 주문 생성 (쿠폰 없음) ===
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(option.getId());
        orderItem.setQuantity(1);
        orderItem.setPrice(product.getPrice());
        orderItem.setCreatedAt(LocalDateTime.now());

        CreateOrderCommand createCommand = new CreateOrderCommand(
                2L,
                List.of(orderItem),
                null  // 쿠폰 없음
        );
        Order order = createOrderUseCase.execute(createCommand);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(10000); // 할인 없음
        assertThat(order.getDiscountAmount()).isEqualTo(0);

        // === 3. 결제 처리 ===
        PayOrderCommand payCommand = new PayOrderCommand(order.getId(), "CARD");
        Order paidOrder = payOrderUseCase.execute(payCommand);

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);

        // 재고 차감 확인
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(49); // 50 - 1
    }

    // === Helper Methods ===

    private Product createProduct(String name, int price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    private ProductOption createProductOption(Long productId, String color, String size, int stock) {
        ProductOption option = new ProductOption();
        option.setProductId(productId);
        option.setColor(color);
        option.setSize(size);
        option.setStock(stock);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        return productOptionRepository.save(option);
    }

    private Coupon createCoupon(String code, int discountAmount, int totalQuantity, int issuedQuantity) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountAmount(discountAmount);
        coupon.setTotalQuantity(totalQuantity);
        coupon.setIssuedQuantity(issuedQuantity);
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidUntil(LocalDateTime.now().plusDays(30));
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        return couponRepository.save(coupon);
    }
}
