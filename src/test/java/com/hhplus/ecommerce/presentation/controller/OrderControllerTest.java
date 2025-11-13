package com.hhplus.ecommerce.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.*;
import com.hhplus.ecommerce.presentation.dto.request.OrderCreateRequestDto;
import com.hhplus.ecommerce.presentation.dto.request.OrderPayRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공")
    void createOrder() throws Exception {
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Black");
        option.setSize("M");
        option.setStock(100);
        ProductOption saved = productOptionRepository.save(option);

        OrderCreateRequestDto.CartItemInfo cartItem = new OrderCreateRequestDto.CartItemInfo(
                1L, saved.getId(), 2, 10000
        );

        OrderCreateRequestDto request = new OrderCreateRequestDto(1L, List.of(cartItem), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/pay - 주문 결제 성공")
    void payOrder() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(20000);
        order.setDiscountAmount(0);
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        OrderPayRequestDto request = new OrderPayRequestDto("PAID");

        mockMvc.perform(post("/api/orders/" + savedOrder.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(savedOrder.getId()));
    }

}
