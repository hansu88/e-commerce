package com.hhplus.ecommerce.controller;

import com.hhplus.ecommerce.order.dto.OrderCreateRequestDto;
import com.hhplus.ecommerce.order.dto.OrderPayRequestDto;
import com.hhplus.ecommerce.order.dto.OrderPayResponseDto;
import com.hhplus.ecommerce.order.dto.OrderResponseDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    public OrderResponseDto createOrder(@RequestBody OrderCreateRequestDto request) {
        // Mock 데이터 예시
        Long mockOrderId = 1001L;
        String mockStatus = "CREATED";
        Long appliedCouponId = request.getCouponId(); // 선택적

        return new OrderResponseDto(mockOrderId, mockStatus, appliedCouponId);
    }

    @PostMapping("/{id}")
    public OrderPayResponseDto payOrder(@PathVariable Long id,
                                        @RequestBody OrderPayRequestDto request) {
        // Mock 결제 처리
        String status = "PAID"; // 성공 시
        return new OrderPayResponseDto(id, status);
    }
}