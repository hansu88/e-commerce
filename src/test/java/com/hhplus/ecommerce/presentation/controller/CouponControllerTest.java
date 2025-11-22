package com.hhplus.ecommerce.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.presentation.dto.request.CouponIssueRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("POST /api/coupons/{id}/issue - 쿠폰 발급 성공")
    void issueCoupon() throws Exception {
        Coupon coupon = Coupon.builder()
                .code("TESTCOUPON")
                .discountAmount(5000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();
        Coupon saved = couponRepository.save(coupon);

        CouponIssueRequestDto request = new CouponIssueRequestDto(1L);

        mockMvc.perform(post("/api/coupons/" + saved.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.couponId").value(saved.getId()));
    }
}
