package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.CancelOrderCommand;
import com.hhplus.ecommerce.application.command.IncreaseStockCommand;
import com.hhplus.ecommerce.application.command.RestoreCouponCommand;
import com.hhplus.ecommerce.application.usecase.coupon.RestoreCouponUseCase;
import com.hhplus.ecommerce.application.usecase.stock.IncreaseStockUseCase;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IncreaseStockUseCase increaseStockUseCase;
    private final RestoreCouponUseCase restoreCouponUseCase;

    public Order execute(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        // 1. 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(command.getOrderId());
        for (OrderItem orderItem : orderItems) {
            IncreaseStockCommand stockCommand = new IncreaseStockCommand(
                orderItem.getProductOptionId(),
                orderItem.getQuantity(),
                StockChangeReason.CANCEL
            );
            increaseStockUseCase.execute(stockCommand);
        }

        // 2. 쿠폰 복구
        if (order.getUserCouponId() != null) {
            RestoreCouponCommand couponCommand = new RestoreCouponCommand(order.getUserCouponId());
            restoreCouponUseCase.execute(couponCommand);
        }

        // 3. 주문 상태 변경
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
