package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
public class OrderServiceTest {
    @Autowired

    private OrderService orderService;
    @Autowired

    private StockService stockService;
    @Autowired

    private CouponService couponService;
    @Autowired

    private ProductOptionRepository productOptionRepository;
    @Autowired

    private OrderItemRepository orderItemRepository;
    @Autowired

    private OrderRepository orderRepository;
    @Autowired

    private StockHistoryRepository stockHistoryRepository;


    

    @Test
    @Transactional
    @DisplayName("주문 생성 시 재고 차감 테스트")
    void createOrderDecreasesStock() {
        // Given - 제품 10개 등록
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Red");
        option.setSize("M");
        option.setStock(10);
        productOptionRepository.save(option);

        OrderItem item = new OrderItem();
        item.setProductOptionId(option.getId());
        item.setQuantity(3);
        item.setPrice(10000);

        // 재고 차감이 목적이라서 쿠폰은 매서드 매개변수 맞춰주기
        Long userCouponId = null;

        // When - 3개 주문
        Order order = orderService.createOrder(1L, List.of(item), userCouponId);

        // Then - 개수와 금액 확인
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(7); // 10 - 3

        assertThat(order.getTotalAmount()).isEqualTo(30000);
        assertThat(order.getStatus()).isEqualTo(com.hhplus.ecommerce.domain.order.OrderStatus.CREATED);
    }

    @Test
    @Transactional
    @DisplayName("재고 부족 시 주문 실패")
    void createOrderOutOfStock() {
        // Given - 상품도록 및 상품 개수가 재고 초과
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Red");
        option.setSize("M");
        option.setStock(2);
        productOptionRepository.save(option);

        OrderItem item = new OrderItem();
        item.setProductOptionId(option.getId());
        item.setQuantity(5);
        item.setPrice(10000);

        Long couponId = null;

        // When & Then - 결과에 대한 오류 발생
        assertThatThrownBy(() -> orderService.createOrder(1L, List.of(item), couponId))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    @Transactional
    @DisplayName("결제 시 재고 변경 없이 상태만 PAID")
    void payOrderChangesStatusOnly() {

        // Given -
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Red");
        option.setSize("M");
        option.setStock(10);
        productOptionRepository.save(option);

        OrderItem item = new OrderItem();
        item.setProductOptionId(option.getId());
        item.setQuantity(3);
        item.setPrice(10000);

        Long couponId = null;

        Order order = orderService.createOrder(1L, List.of(item), couponId);

        // When - 결제완료 처리 , 신용카드로 하였음 (이미 차감은 진행된 상태)
        orderService.payOrder(order.getId(), "CREDIT_CARD");

        Order paidOrder = orderRepository.findById(order.getId()).orElse(order);
        assertThat(paidOrder.getStatus()).isEqualTo(com.hhplus.ecommerce.domain.order.OrderStatus.PAID);

        // 재고는 이미 차감 완료 상태
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getStock()).isEqualTo(7);
    }
}
