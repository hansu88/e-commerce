package com.hhplus.ecommerce.controller;

import com.hhplus.ecommerce.coupon.dto.CouponIssueRequestDto;
import com.hhplus.ecommerce.coupon.dto.CouponIssueResponseDto;
import com.hhplus.ecommerce.coupon.dto.MyCouponResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @PostMapping("/{id}/issue")
    public CouponIssueResponseDto issueCoupon(@PathVariable Long id,
                                              @RequestBody CouponIssueRequestDto request) {
        return new CouponIssueResponseDto(/*mock*/);
    }

    @GetMapping("/my")
    public List<MyCouponResponseDto> getMyCoupons(@RequestParam Long uid,
                                                  @RequestParam String state) {
        return List.of(new MyCouponResponseDto(/*mock*/));
    }
}