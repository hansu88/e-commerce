package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.service.OrderService;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.presentation.dto.request.OrderCreateRequestDto;
import com.hhplus.ecommerce.presentation.dto.request.OrderPayRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.OrderPayResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.OrderResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderCreateRequestDto request) {
        List<OrderItem> orderItems = request.getCartItems().stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductOptionId(item.getProductOptionId());
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setPrice(item.getPrice());
                    return orderItem;
                })
                .collect(Collectors.toList());

        Order order = orderService.createOrder(request.getUserId(), orderItems, request.getCouponId());

        OrderResponseDto response = new OrderResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getUserCouponId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderPayResponseDto> payOrder(
            @PathVariable Long id,
            @RequestBody OrderPayRequestDto request) {

        Order order = orderService.payOrder(id, request.getPaymentMethod());

        OrderPayResponseDto response = new OrderPayResponseDto(order.getId(), order.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long id) {
        Order order = orderService.cancelOrder(id);

        OrderResponseDto response = new OrderResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getUserCouponId()
        );

        return ResponseEntity.ok(response);
    }
}
