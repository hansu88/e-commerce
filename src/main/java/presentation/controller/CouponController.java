package presentation.controller;

import presentation.dto.response.ErrorResponse;
import presentation.dto.request.CouponIssueRequestDto;
import presentation.dto.response.CouponIssueResponseDto;
import presentation.dto.response.MyCouponResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    // Mock 데이터 저장소
    private static final AtomicLong USER_COUPON_ID_GENERATOR = new AtomicLong(1);

    // 쿠폰 정보 (couponId -> CouponInfo)
    private static final Map<Long, CouponInfo> COUPONS = new ConcurrentHashMap<>() {{
        put(1L, new CouponInfo(1L, "신규가입 5천원 할인", 5000, 100, new AtomicInteger(100), "2025-12-31"));
        put(2L, new CouponInfo(2L, "첫 구매 1만원 할인", 10000, 50, new AtomicInteger(50), "2025-12-31"));
        put(3L, new CouponInfo(3L, "VIP 3천원 할인", 3000, 10, new AtomicInteger(2), "2025-11-30")); // 거의 소진
        put(4L, new CouponInfo(4L, "블랙프라이데이 2만원 할인", 20000, 200, new AtomicInteger(0), "2025-11-30")); // 완전 소진
    }};

    // 사용자별 발급된 쿠폰 (userId -> List<UserCoupon>)
    private static final Map<Long, List<UserCoupon>> USER_COUPONS = new ConcurrentHashMap<>();

    // 쿠폰 정보 클래스
    private static class CouponInfo {
        Long couponId;
        String name;
        Integer discountAmount;
        Integer totalQuantity;
        AtomicInteger remainingQuantity;
        String validUntil;

        CouponInfo(Long couponId, String name, Integer discountAmount, Integer totalQuantity,
                   AtomicInteger remainingQuantity, String validUntil) {
            this.couponId = couponId;
            this.name = name;
            this.discountAmount = discountAmount;
            this.totalQuantity = totalQuantity;
            this.remainingQuantity = remainingQuantity;
            this.validUntil = validUntil;
        }
    }

    // 사용자 쿠폰 클래스
    private static class UserCoupon {
        Long userCouponId;
        Long userId;
        Long couponId;
        String issuedAt;
        String status; // AVAILABLE, USED, EXPIRED

        UserCoupon(Long userCouponId, Long userId, Long couponId, String issuedAt, String status) {
            this.userCouponId = userCouponId;
            this.userId = userId;
            this.couponId = couponId;
            this.issuedAt = issuedAt;
            this.status = status;
        }
    }

    static {
        // 초기 Mock 데이터: userId 1번의 쿠폰
        List<UserCoupon> user1Coupons = new ArrayList<>();
        user1Coupons.add(new UserCoupon(1L, 1L, 1L, "2025-10-01T10:00:00", "AVAILABLE"));
        user1Coupons.add(new UserCoupon(2L, 1L, 2L, "2025-10-01T10:00:00", "USED"));
        user1Coupons.add(new UserCoupon(3L, 1L, 3L, "2025-09-01T10:00:00", "EXPIRED"));
        USER_COUPONS.put(1L, user1Coupons);
    }

    @Operation(
            summary = "쿠폰 발급",
            description = "선착순으로 쿠폰을 발급받습니다. 수량이 제한되어 있으며, 중복 발급이 불가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "쿠폰 발급 성공",
                    content = @Content(schema = @Schema(implementation = CouponIssueResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "쿠폰 발급 실패 (이미 발급됨, 수량 소진)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueCoupon(
            @Parameter(description = "쿠폰 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody CouponIssueRequestDto request) {

        // 실패 케이스 1: 존재하지 않는 쿠폰
        CouponInfo coupon = COUPONS.get(id);
        if (coupon == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다. (ID: " + id + ")"));
        }

        // 실패 케이스 2: 이미 발급받은 쿠폰 (중복 발급 방지)
        List<UserCoupon> userCoupons = USER_COUPONS.getOrDefault(request.getUserId(), new ArrayList<>());
        boolean alreadyIssued = userCoupons.stream()
                .anyMatch(uc -> uc.couponId.equals(id));

        if (alreadyIssued) {
            return ResponseEntity.status(409)
                    .body(new ErrorResponse("COUPON_ALREADY_ISSUED",
                            "이미 발급받은 쿠폰입니다. (쿠폰 ID: " + id + ")"));
        }

        // 실패 케이스 3: 쿠폰 수량 소진 (선착순 마감)
        // AtomicInteger를 사용하여 동시성 제어
        int remaining = coupon.remainingQuantity.decrementAndGet();
        if (remaining < 0) {
            // 수량이 부족하면 다시 원복
            coupon.remainingQuantity.incrementAndGet();
            return ResponseEntity.status(409)
                    .body(new ErrorResponse("COUPON_SOLD_OUT",
                            "쿠폰이 모두 소진되었습니다. (쿠폰 ID: " + id + ", 총 수량: " + coupon.totalQuantity + ")"));
        }

        // 성공 케이스: 쿠폰 발급
        Long userCouponId = USER_COUPON_ID_GENERATOR.getAndIncrement();
        String issuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        UserCoupon newUserCoupon = new UserCoupon(userCouponId, request.getUserId(), id, issuedAt, "AVAILABLE");

        USER_COUPONS.putIfAbsent(request.getUserId(), new ArrayList<>());
        USER_COUPONS.get(request.getUserId()).add(newUserCoupon);

        CouponIssueResponseDto response = new CouponIssueResponseDto(id, issuedAt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 쿠폰 조회",
            description = "사용자가 발급받은 쿠폰 목록을 조회합니다. 상태별 필터링이 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyCouponResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 상태 값)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<?> getMyCoupons(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long uid,
            @Parameter(description = "쿠폰 상태 필터", example = "AVAILABLE",
                    schema = @Schema(allowableValues = {"AVAILABLE", "USED", "EXPIRED", "ALL"}))
            @RequestParam(required = false, defaultValue = "ALL") String state) {

        // 실패 케이스: 잘못된 상태 값
        List<String> validStates = List.of("AVAILABLE", "USED", "EXPIRED", "ALL");
        if (!validStates.contains(state)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_STATE",
                            "잘못된 상태 값입니다. (입력값: " + state + ", 허용값: " + String.join(", ", validStates) + ")"));
        }

        // 성공 케이스: 쿠폰 목록 조회
        List<UserCoupon> userCoupons = USER_COUPONS.getOrDefault(uid, new ArrayList<>());

        // 상태별 필터링
        List<MyCouponResponseDto> filteredCoupons = userCoupons.stream()
                .filter(uc -> "ALL".equals(state) || uc.status.equals(state))
                .map(uc -> {
                    CouponInfo coupon = COUPONS.get(uc.couponId);
                    return new MyCouponResponseDto(
                            uc.userCouponId,
                            uc.couponId,
                            coupon != null ? coupon.discountAmount : 0,
                            coupon != null ? coupon.validUntil : "",
                            uc.status
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredCoupons);
    }
}