package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.PayOrderCommand;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayOrderUseCase {

    private final OrderRepository orderRepository;

    public Order execute(PayOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
}
