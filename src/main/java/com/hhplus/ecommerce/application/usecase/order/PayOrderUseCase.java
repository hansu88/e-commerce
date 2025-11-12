package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.order.PayOrderCommand;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PayOrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional
    public Order execute(PayOrderCommand command) {
        command.validate();
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
}
