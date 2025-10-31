package com.hhplus.ecommerce.controller;

import com.hhplus.ecommerce.coupon.dto.CouponIssueRequestDto;
import com.hhplus.ecommerce.coupon.dto.CouponIssueResponseDto;
import com.hhplus.ecommerce.coupon.dto.MyCouponResponseDto;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @PostMapping("/{id}/issue")
    public CouponIssueResponseDto issueCoupon(@PathVariable Long id,
                                              @RequestBody CouponIssueRequestDto request) {
        // Mock 발급 시간
        String issuedAt = LocalDateTime.now().toString();

        return new CouponIssueResponseDto(id, issuedAt);
    }

    @GetMapping("/my")
    public List<MyCouponResponseDto> getMyCoupons(@RequestParam Long uid,
                                                  @RequestParam String state) {
        // Mock 데이터 예시
        return List.of(
                new MyCouponResponseDto(1L, 101L, 5000, "2025-12-31", "AVAILABLE"),
                new MyCouponResponseDto(2L, 102L, 10000, "2025-11-30", "USED")
        );
    }
}