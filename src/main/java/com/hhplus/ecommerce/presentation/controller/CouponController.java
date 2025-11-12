package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.coupon.GetMyCouponsCommand;
import com.hhplus.ecommerce.application.command.coupon.IssueCouponCommand;
import com.hhplus.ecommerce.application.usecase.coupon.GetMyCouponsUseCase;
import com.hhplus.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.presentation.dto.request.CouponIssueRequestDto;
import com.hhplus.ecommerce.presentation.dto.response.coupon.CouponIssueResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.coupon.MyCouponResponseDto;
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
    private final CouponRepository couponRepository;

    /** 쿠폰 발급 */
    @PostMapping("/{id}/issue")
    public ResponseEntity<CouponIssueResponseDto> issueCoupon(
            @PathVariable Long id,
            @RequestBody CouponIssueRequestDto request) {

        IssueCouponCommand command = new IssueCouponCommand(request.getUserId(), id);
        command.validate();

        UserCoupon userCoupon = issueCouponUseCase.execute(command);

        String issuedAt = userCoupon.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        CouponIssueResponseDto response = new CouponIssueResponseDto(id, issuedAt);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 내 쿠폰 조회 */
    @GetMapping("/my")
    public ResponseEntity<List<MyCouponResponseDto>> getMyCoupons(@RequestParam Long uid) {

        // Command 생성 후 유효성 체크
        GetMyCouponsCommand command = new GetMyCouponsCommand(uid);
        command.validate();

        List<UserCoupon> userCoupons = getMyCouponsUseCase.execute(command);

        // UserCoupon + Coupon 엔티티 매핑
        List<MyCouponResponseDto> response = userCoupons.stream()
                .map(uc -> {
                    Coupon coupon = couponRepository.findById(uc.getCouponId())
                            .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
                    return new MyCouponResponseDto(
                            uc.getId(),
                            uc.getCouponId(),
                            coupon.getDiscountAmount(),
                            coupon.getValidUntil().toString(),
                            uc.getStatus().name()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
