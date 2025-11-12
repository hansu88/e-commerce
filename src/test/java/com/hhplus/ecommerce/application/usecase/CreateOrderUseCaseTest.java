package com.hhplus.ecommerce.application.usecase;

import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.command.order.PayOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.PayOrderUseCase;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateOrderUseCaseTest {
    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private PayOrderUseCase payOrderUseCase;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
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

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(item), null);

        // When - 3개 주문
        Order order = createOrderUseCase.execute(command);

        // Then - 개수와 금액 확인
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertAll(
            () -> assertThat(updatedOption.getStock()).isEqualTo(7), // 10 - 3
            () -> assertThat(order.getTotalAmount()).isEqualTo(30000),
            () -> assertThat(order.getStatus()).isEqualTo(com.hhplus.ecommerce.domain.order.OrderStatus.CREATED)
        );
    }

    @Test
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

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(item), null);

        // When & Then - 결과에 대한 오류 발생
        assertThatThrownBy(() -> createOrderUseCase.execute(command))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    @DisplayName("결제 시 재고 변경 없이 상태만 PAID")
    void payOrderChangesStatusOnly() {
        // Given
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

        CreateOrderCommand createCommand = new CreateOrderCommand(1L, List.of(item), null);
        Order order = createOrderUseCase.execute(createCommand);

        // When - 결제완료 처리 , 신용카드로 하였음 (이미 차감은 진행된 상태)
        PayOrderCommand payCommand = new PayOrderCommand(order.getId(), "CREDIT_CARD");
        payOrderUseCase.execute(payCommand);

        Order paidOrder = orderRepository.findById(order.getId()).orElse(order);
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();

        assertAll(
            () -> assertThat(paidOrder.getStatus()).isEqualTo(com.hhplus.ecommerce.domain.order.OrderStatus.PAID),
            () -> assertThat(updatedOption.getStock()).isEqualTo(7) // 재고는 이미 차감 완료 상태
        );
    }
}
