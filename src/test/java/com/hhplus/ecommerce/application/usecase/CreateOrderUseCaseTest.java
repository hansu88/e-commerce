package com.hhplus.ecommerce.application.usecase;

import com.hhplus.ecommerce.application.command.CreateOrderCommand;
import com.hhplus.ecommerce.application.command.DecreaseStockCommand;
import com.hhplus.ecommerce.application.command.PayOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.PayOrderUseCase;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryCouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryOrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryOrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryStockHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryUserCouponRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class CreateOrderUseCaseTest {
    private CreateOrderUseCase createOrderUseCase;
    private PayOrderUseCase payOrderUseCase;
    private DecreaseStockUseCase decreaseStockUseCase;
    private UseCouponUseCase useCouponUseCase;
    private InMemoryProductOptionRepository productOptionRepository;
    private InMemoryOrderItemRepository orderItemRepository;
    private InMemoryOrderRepository orderRepository;
    private InMemoryStockHistoryRepository stockHistoryRepository;
    private InMemoryCouponRepository couponRepository;
    private InMemoryUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        productOptionRepository = new InMemoryProductOptionRepository();
        stockHistoryRepository = new InMemoryStockHistoryRepository();
        orderItemRepository = new InMemoryOrderItemRepository();
        orderRepository = new InMemoryOrderRepository();
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();

        decreaseStockUseCase = new DecreaseStockUseCase(productOptionRepository, stockHistoryRepository);
        useCouponUseCase = Mockito.mock(UseCouponUseCase.class);

        createOrderUseCase = new CreateOrderUseCase(
            orderRepository,
            orderItemRepository,
            decreaseStockUseCase,
            useCouponUseCase,
            couponRepository,
            userCouponRepository
        );

        payOrderUseCase = new PayOrderUseCase(orderRepository);
    }

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
