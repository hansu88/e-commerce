package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.order.CancelOrderCommand;
import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.command.order.PayOrderCommand;
import com.hhplus.ecommerce.application.usecase.order.CancelOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.usecase.order.PayOrderUseCase;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.presentation.dto.request.OrderCreateRequestDto;
import com.hhplus.ecommerce.presentation.dto.request.OrderPayRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.order.OrderPayResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.order.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final PayOrderUseCase payOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderCreateRequestDto request) {
        List<OrderItem> orderItems = request.getCartItems().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductOptionId(i.getProductOptionId());
            item.setQuantity(i.getQuantity());
            item.setPrice(i.getPrice());
            return item;
        }).collect(Collectors.toList());

        CreateOrderCommand command = new CreateOrderCommand(request.getUserId(), orderItems, request.getCouponId());
        Order order = createOrderUseCase.execute(command);

        OrderResponseDto response = new OrderResponseDto(order.getId(), order.getStatus().name(), order.getUserCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderPayResponseDto> payOrder(@PathVariable Long id, @RequestBody OrderPayRequestDto request) {
        PayOrderCommand command = new PayOrderCommand(id, request.getPaymentMethod());
        Order order = payOrderUseCase.execute(command);
        return ResponseEntity.ok(new OrderPayResponseDto(order.getId(), order.getStatus().name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long id) {
        CancelOrderCommand command = new CancelOrderCommand(id);
        Order order = cancelOrderUseCase.execute(command);
        return ResponseEntity.ok(new OrderResponseDto(order.getId(), order.getStatus().name(), order.getUserCouponId()));
    }
}
