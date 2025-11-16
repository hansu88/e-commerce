package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.command.cart.AddCartItemCommand;
import com.hhplus.ecommerce.application.command.cart.GetCartItemsCommand;
import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.command.product.GetProductDetailCommand;
import com.hhplus.ecommerce.application.usecase.cart.AddCartItemUseCase;
import com.hhplus.ecommerce.application.usecase.cart.GetCartItemsUseCase;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductDetailUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductListUseCase;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.*;
import com.hhplus.ecommerce.presentation.dto.response.cart.CartItemResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * E-Commerce 통합 테스트
 * - 상품 조회 -> 장바구니 담기 -> 쿠폰 발급 -> 주문 생성 전체 플로우 검증
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ECommerceIntegrationTest {

    @Autowired
    private GetProductListUseCase getProductListUseCase;

    @Autowired
    private GetProductDetailUseCase getProductDetailUseCase;

    @Autowired
    private AddCartItemUseCase addCartItemUseCase;

    @Autowired
    private GetCartItemsUseCase getCartItemsUseCase;

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("통합 테스트 1: 상품 조회 -> 장바구니 담기 -> 주문 생성 전체 플로우")
    void ecommerce_full_flow_without_coupon() {
        // Given - 상품 및 옵션 생성
        Product product = createProduct("테스트 상품", 10000, ProductStatus.ACTIVE);
        ProductOption option = createProductOption(product.getId(), "RED", "L", 100);

        Long userId = 1L;

        // When & Then 1 - 상품 목록 조회
        List<ProductListResponseDto> productList = getProductListUseCase.execute();
        assertThat(productList).hasSize(1);
        assertThat(productList.get(0).name()).isEqualTo("테스트 상품");
        assertThat(productList.get(0).totalStock()).isEqualTo(100);

        // When & Then 2 - 상품 상세 조회
        GetProductDetailCommand detailCommand = new GetProductDetailCommand(product.getId());
        ProductDetailResponseDto productDetail = getProductDetailUseCase.execute(detailCommand);
        assertThat(productDetail.name()).isEqualTo("테스트 상품");
        assertThat(productDetail.options()).hasSize(1);

        // When & Then 3 - 장바구니 담기
        AddCartItemCommand addCartCommand = new AddCartItemCommand(userId, option.getId(), 2);
        CartItem cartItem = addCartItemUseCase.execute(addCartCommand);
        assertThat(cartItem.getId()).isNotNull();

        // When & Then 4 - 장바구니 조회
        GetCartItemsCommand getCartCommand = new GetCartItemsCommand(userId);
        List<CartItemResponseDto> cartItems = getCartItemsUseCase.execute(getCartCommand);
        assertThat(cartItems).hasSize(1);
        assertThat(cartItems.get(0).quantity()).isEqualTo(2);

        // When & Then 5 - 주문 생성 (쿠폰 없이)
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(option.getId());
        orderItem.setQuantity(2);
        orderItem.setPrice(10000);
        orderItems.add(orderItem);

        CreateOrderCommand orderCommand = new CreateOrderCommand(userId, orderItems, null);
        Order order = createOrderUseCase.execute(orderCommand);

        assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getTotalAmount()).isEqualTo(20000), // 10000 * 2
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0)
        );

        // 재고 감소 확인
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(98); // 100 - 2

        // 주문 항목 확인
        assertThat(orderItemRepository.findByOrderId(order.getId())).hasSize(1);
    }

    @Test
    @DisplayName("통합 테스트 2: 쿠폰 발급 후 주문 생성 (할인 적용)")
    void ecommerce_full_flow_with_coupon() {
        // Given - 상품, 옵션, 쿠폰 생성
        Product product = createProduct("할인 상품", 50000, ProductStatus.ACTIVE);
        ProductOption option = createProductOption(product.getId(), "BLUE", "M", 50);
        Coupon coupon = createCoupon("WELCOME10", 10000, 100, 0);

        Long userId = 2L;

        // When & Then 1 - 쿠폰 발급
        IssueCouponCommand issueCouponCommand = new IssueCouponCommand(userId, coupon.getId());
        UserCoupon userCoupon = issueCouponUseCase.execute(issueCouponCommand);
        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCouponId()).isEqualTo(coupon.getId());

        // When & Then 2 - 장바구니 담기
        AddCartItemCommand addCartCommand = new AddCartItemCommand(userId, option.getId(), 1);
        addCartItemUseCase.execute(addCartCommand);

        // When & Then 3 - 주문 생성 (쿠폰 사용)
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductOptionId(option.getId());
        orderItem.setQuantity(1);
        orderItem.setPrice(50000);
        orderItems.add(orderItem);

        CreateOrderCommand orderCommand = new CreateOrderCommand(userId, orderItems, userCoupon.getId());
        Order order = createOrderUseCase.execute(orderCommand);

        assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getTotalAmount()).isEqualTo(40000), // 50000 - 10000
                () -> assertThat(order.getDiscountAmount()).isEqualTo(10000),
                () -> assertThat(order.getUserCouponId()).isEqualTo(userCoupon.getId())
        );
    }

    @Test
    @DisplayName("통합 테스트 3: 여러 옵션 장바구니 담기 후 주문")
    void ecommerce_multiple_options_order() {
        // Given - 상품 1개에 여러 옵션
        Product product = createProduct("다양한 옵션 상품", 30000, ProductStatus.ACTIVE);
        ProductOption option1 = createProductOption(product.getId(), "BLACK", "S", 10);
        ProductOption option2 = createProductOption(product.getId(), "WHITE", "L", 20);

        Long userId = 3L;

        // When - 여러 옵션 장바구니 담기
        addCartItemUseCase.execute(new AddCartItemCommand(userId, option1.getId(), 2));
        addCartItemUseCase.execute(new AddCartItemCommand(userId, option2.getId(), 3));

        // Then - 장바구니 확인
        GetCartItemsCommand getCartCommand = new GetCartItemsCommand(userId);
        List<CartItemResponseDto> cartItems = getCartItemsUseCase.execute(getCartCommand);
        assertThat(cartItems).hasSize(2);

        // When - 주문 생성
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        item1.setProductOptionId(option1.getId());
        item1.setQuantity(2);
        item1.setPrice(30000);
        orderItems.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setProductOptionId(option2.getId());
        item2.setQuantity(3);
        item2.setPrice(30000);
        orderItems.add(item2);

        CreateOrderCommand orderCommand = new CreateOrderCommand(userId, orderItems, null);
        Order order = createOrderUseCase.execute(orderCommand);

        // Then - 주문 검증
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(150000), // (30000*2) + (30000*3)
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED)
        );

        // 재고 확인
        ProductOption updatedOption1 = productOptionRepository.findById(option1.getId()).orElseThrow();
        ProductOption updatedOption2 = productOptionRepository.findById(option2.getId()).orElseThrow();
        assertThat(updatedOption1.getStock()).isEqualTo(8);  // 10 - 2
        assertThat(updatedOption2.getStock()).isEqualTo(17); // 20 - 3

        // 주문 항목 확인
        assertThat(orderItemRepository.findByOrderId(order.getId())).hasSize(2);
    }

    // Helper methods
    private Product createProduct(String name, int price, ProductStatus status) {
        Product product = new Product(name, price, status);
        return productRepository.save(product);
    }

    private ProductOption createProductOption(Long productId, String color, String size, int stock) {
        ProductOption option = new ProductOption();
        option.setProductId(productId);
        option.setColor(color);
        option.setSize(size);
        option.setStock(stock);
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
        return couponRepository.save(coupon);
    }
}
