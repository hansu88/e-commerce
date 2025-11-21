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

        // 재고 차감
        for (OrderItem orderItem : command.getOrderItems()) {
            DecreaseStockCommand stockCommand = new DecreaseStockCommand(
                    orderItem.getProductOptionId(),
                    orderItem.getQuantity(),
                    StockChangeReason.ORDER
            );
            decreaseStockUseCase.execute(stockCommand);
        }

        // 쿠폰 사용
        int discountAmount = 0;
        if (command.getUserCouponId() != null) {
            UseCouponCommand couponCommand = new UseCouponCommand(command.getUserCouponId());
            discountAmount = useCouponUseCase.execute(couponCommand);
        }

        // 주문 생성
        Order order = new Order();
        order.setUserId(command.getUserId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserCouponId(command.getUserCouponId());

        int subtotal = command.getOrderItems().stream().mapToInt(i -> i.getQuantity() * i.getPrice()).sum();
        int finalAmount = Math.max(subtotal - discountAmount, 0);

        order.setTotalAmount(finalAmount);
        order.setDiscountAmount(discountAmount);

        Order savedOrder = orderRepository.save(order);

        // OrderItem Batch Insert (N번 INSERT → 1번 Batch INSERT)
        command.getOrderItems().forEach(item -> item.setOrderId(savedOrder.getId()));
        orderItemRepository.saveAll(command.getOrderItems());

        return savedOrder;
    }
}
