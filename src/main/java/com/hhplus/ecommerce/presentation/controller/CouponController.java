package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.service.CouponService;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.presentation.dto.request.CouponIssueRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.CouponIssueResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<CouponIssueResponseDto> issueCoupon(
            @PathVariable Long id,
            @RequestBody CouponIssueRequestDto request) {

        UserCoupon userCoupon = couponService.issueCoupon(request.getUserId(), id);

        String issuedAt = userCoupon.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        CouponIssueResponseDto response = new CouponIssueResponseDto(id, issuedAt);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
