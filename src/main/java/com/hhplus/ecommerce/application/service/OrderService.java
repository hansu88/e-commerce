package com.hhplus.ecommerce.application.service;

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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @deprecated Use UseCase pattern instead:
 * - {@link com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase} for creating orders
 * - {@link com.hhplus.ecommerce.application.usecase.order.PayOrderUseCase} for payment
 * - {@link com.hhplus.ecommerce.application.usecase.order.CancelOrderUseCase} for cancellation
 */
@Deprecated
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockService stockService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public OrderService(OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       StockService stockService,
                       CouponService couponService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockService = stockService;
        this.couponService = couponService;
        this.couponRepository = couponService.getCouponRepository();
        this.userCouponRepository = couponService.getUserCouponRepository();
    }

    /**
     * 주문 생성 (재고 차감, 쿠폰 적용)
     */
    public Order createOrder(Long userId, List<OrderItem> orderItems, Long userCouponId) {

        // 1. 재고 차감
        for (OrderItem orderItem : orderItems) {
            stockService.decreaseStock(orderItem.getProductOptionId(), orderItem.getQuantity(), StockChangeReason.ORDER);
        }

        // 2. 쿠폰 할인 금액 계산 및 사용
        int discountAmount = 0;
        if (userCouponId != null) {
            UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰이 존재하지 않습니다."));

            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

            discountAmount = coupon.getDiscountAmount();
            couponService.useCoupon(userCouponId);
        }

        // 3. 주문 생성
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserCouponId(userCouponId);

        // 4. 총 금액 계산 (할인 적용)
        int subtotal = orderItems.stream()
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
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(savedOrder.getId());
            orderItemRepository.save(orderItem);
        }

        return savedOrder;
    }

    /**
     * 주문 결제 (상태 변경)
     */
    public Order payOrder(Long orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    /**
     * 주문 취소 (재고 복구, 쿠폰 복구)
     */
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        // 1. 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            stockService.increaseStock(orderItem.getProductOptionId(), orderItem.getQuantity(), StockChangeReason.CANCEL);
        }

        // 2. 쿠폰 복구
        if (order.getUserCouponId() != null) {
            couponService.restoreCoupon(order.getUserCouponId());
        }

        // 3. 주문 상태 변경
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
