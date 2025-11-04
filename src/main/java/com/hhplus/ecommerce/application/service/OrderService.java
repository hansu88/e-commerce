package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderRepository;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final StockService stockService;
    private OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository, StockService stockService) {
        this.orderRepository = orderRepository;
        this.stockService = stockService;
    }

    /**
     * 재고 증감 ,차감
     */
    public Order createOrder(Long userId, List<OrderItem> orderItems ) {

        // 사용자가 고른 재고 차감
        for (OrderItem orderItem : orderItems) {
            stockService.decreaseStock(orderItem.getProductOptionId(), orderItem.getQuantity(), StockChangeReason.ORDER);
        }

        // 주문 생성
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        // 총 금액
        int totalAmount = orderItems.stream()
                        .mapToInt(i -> i.getQuantity() * i.getPrice()).sum();
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }
    
    // 결제시에는 상태 변경
    // 추후 시간에 따라 다시 증감 작업 예정
    public Order payOrder(Long orderId, String paymentMethod) {
        Order order  = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAID);
        return  orderRepository.save(order);
    }
    
}
