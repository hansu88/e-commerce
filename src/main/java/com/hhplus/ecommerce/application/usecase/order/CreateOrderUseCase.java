package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.application.command.coupon.UseCouponCommand;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DecreaseStockUseCase decreaseStockUseCase;
    private final UseCouponUseCase useCouponUseCase;


    @Transactional
    public Order execute(CreateOrderCommand command) {
        command.validate();

        // 1. 재고 차감
        for (OrderItem item : command.getOrderItems()) {
            decreaseStockUseCase.execute(new DecreaseStockCommand(
                    item.getProductOptionId(), item.getQuantity(), StockChangeReason.ORDER
            ));
        }

        // 2. 쿠폰 사용 처리
        if (command.getUserCouponId() != null) {
            useCouponUseCase.execute(new UseCouponCommand(command.getUserCouponId()));
        }

        // 3. 주문 생성
        Order order = new Order();
        order.setUserId(command.getUserId());
        order.setStatus(OrderStatus.CREATED);
        order.setUserCouponId(command.getUserCouponId());
        order.setCreatedAt(LocalDateTime.now());

        int totalAmount = command.getOrderItems().stream().mapToInt(i -> i.getQuantity() * i.getPrice()).sum();
        order.setTotalAmount(totalAmount); // discount 처리도 필요하면 반영
        return orderRepository.save(order);
    }
}
