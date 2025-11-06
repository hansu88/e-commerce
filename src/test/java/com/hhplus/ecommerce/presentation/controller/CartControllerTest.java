package com.hhplus.ecommerce.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.cart.Cart;
import com.hhplus.ecommerce.domain.cart.CartItem;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryCartItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryCartRepository;
import com.hhplus.ecommerce.presentation.dto.request.CartAddRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryCartRepository cartRepository;

    @Autowired
    private InMemoryCartItemRepository cartItemRepository;

    @Test
    @DisplayName("POST /api/carts - 장바구니 담기 성공")
    void addCart() throws Exception {
        CartAddRequestDto request = new CartAddRequestDto(1L, 1L, 2);

        mockMvc.perform(post("/api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists());
    }

    @Test
    @DisplayName("GET /api/carts?uid={uid} - 장바구니 조회 성공")
    void getCart() throws Exception {
        Cart cart = new Cart();
        cart.setUserId(1L);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCartId(savedCart.getId());
        cartItem.setProductOptionId(1L);
        cartItem.setQuantity(2);
        cartItemRepository.save(cartItem);

        mockMvc.perform(get("/api/carts")
                        .param("uid", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /api/carts/{id} - 장바구니 항목 삭제 성공")
    void deleteCartItem() throws Exception {
        Cart cart = new Cart();
        cart.setUserId(1L);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCartId(savedCart.getId());
        cartItem.setProductOptionId(1L);
        cartItem.setQuantity(2);
        CartItem savedItem = cartItemRepository.save(cartItem);

        mockMvc.perform(delete("/api/carts/" + savedItem.getId()))
                .andExpect(status().isNoContent());
    }
}
