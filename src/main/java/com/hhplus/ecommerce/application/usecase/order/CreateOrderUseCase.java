package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.application.command.DecreaseStockCommand;
import com.hhplus.ecommerce.application.command.UseCouponCommand;
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

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DecreaseStockUseCase decreaseStockUseCase;
    private final UseCouponUseCase useCouponUseCase;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public Order execute(CreateOrderCommand command) {
        // 1. 재고 차감
        for (OrderItem orderItem : command.getOrderItems()) {
            DecreaseStockCommand stockCommand = new DecreaseStockCommand(
                orderItem.getProductOptionId(),
                orderItem.getQuantity(),
                StockChangeReason.ORDER
            );
            decreaseStockUseCase.execute(stockCommand);
        }

        // 2. 쿠폰 할인 금액 계산 및 사용
        int discountAmount = 0;
        if (command.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findById(command.getUserCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

            discountAmount = coupon.getDiscountAmount();

            UseCouponCommand couponCommand = new UseCouponCommand(command.getUserCouponId());
            useCouponUseCase.execute(couponCommand);
        }

        // 3. 주문 생성
        Order order = new Order();
        order.setUserId(command.getUserId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserCouponId(command.getUserCouponId());

        // 4. 총 금액 계산 (할인 적용)
        int subtotal = command.getOrderItems().stream()
                .mapToInt(i -> i.getQuantity() * i.getPrice())
                .sum();

        int finalAmount = subtotal - discountAmount;
        if (finalAmount < 0) {
            finalAmount = 0;
        }

        order.setTotalAmount(finalAmount);
        order.setDiscountAmount(discountAmount);

        // 5. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 6. 주문 아이템 저장
        for (OrderItem orderItem : command.getOrderItems()) {
            orderItem.setOrderId(savedOrder.getId());
            orderItemRepository.save(orderItem);
        }

        return savedOrder;
    }
}
