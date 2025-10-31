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
        return new OrderResponseDto(/*mock data*/);
    }

    @PostMapping("/{id}")
    public OrderPayResponseDto payOrder(@PathVariable Long id,
                                        @RequestBody OrderPayRequestDto request) {
        return new OrderPayResponseDto(/*mock data*/);
    }
}