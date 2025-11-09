package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.GetMyCouponsCommand;
import com.hhplus.ecommerce.application.command.IssueCouponCommand;
import com.hhplus.ecommerce.application.usecase.coupon.GetMyCouponsUseCase;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.presentation.dto.request.CouponIssueRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.CouponIssueResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.MyCouponResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetMyCouponsUseCase getMyCouponsUseCase;

    @PostMapping("/{id}/issue")
    public ResponseEntity<CouponIssueResponseDto> issueCoupon(
            @PathVariable Long id,
            @RequestBody CouponIssueRequestDto request) {

        IssueCouponCommand command = new IssueCouponCommand(request.getUserId(), id);
        UserCoupon userCoupon = issueCouponUseCase.execute(command);

        String issuedAt = userCoupon.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        CouponIssueResponseDto response = new CouponIssueResponseDto(id, issuedAt);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyCouponResponseDto>> getMyCoupons(@RequestParam Long uid) {
        GetMyCouponsCommand command = new GetMyCouponsCommand(uid);
        List<UserCoupon> userCoupons = getMyCouponsUseCase.execute(command);

        List<MyCouponResponseDto> response = userCoupons.stream()
                .map(uc -> new MyCouponResponseDto(
                        uc.getId(),
                        uc.getCouponId(),
                        0, // discountAmount - 추후 Coupon 정보 조회 필요
                        null, // validUntil - 추후 Coupon 정보 조회 필요
                        uc.getStatus().name()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
