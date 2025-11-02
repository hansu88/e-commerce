package presentation.controller;

import presentation.dto.response.ErrorResponse;
import presentation.dto.request.OrderCreateRequestDto;
import presentation.dto.request.OrderPayRequestDto;
import presentation.dto.response.OrderPayResponseDto;
import presentation.dto.response.OrderResponseDto;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문/결제 관리 API")
public class OrderController {

    // Mock 데이터 저장소
    private static final AtomicLong ORDER_ID_GENERATOR = new AtomicLong(1001);
    private static final Map<Long, MockOrder> ORDERS = new ConcurrentHashMap<>();

    // 쿠폰 정보 (쿠폰 ID -> 할인 금액)
    private static final Map<Long, Integer> COUPONS = new HashMap<>() {{
        put(1L, 5000);   // 5천원 할인
        put(2L, 10000);  // 1만원 할인
        put(3L, 3000);   // 3천원 할인
    }};

    // 사용된 쿠폰 추적 (userId -> 사용한 쿠폰 ID 리스트)
    private static final Map<Long, Set<Long>> USED_COUPONS = new ConcurrentHashMap<>();

    // 사용자 잔액 (userId -> 잔액)
    private static final Map<Long, Integer> USER_BALANCE = new HashMap<>() {{
        put(1L, 100000);  // 10만원
        put(2L, 50000);   // 5만원
        put(3L, 10000);   // 1만원 (부족)
    }};

    // Mock Order 클래스
    private static class MockOrder {
        Long orderId;
        Long userId;
        String status; // CREATED, PAID, CANCELLED
        Integer totalAmount;
        Long appliedCouponId;
        String paymentMethod;

        MockOrder(Long orderId, Long userId, String status, Integer totalAmount, Long appliedCouponId) {
            this.orderId = orderId;
            this.userId = userId;
            this.status = status;
            this.totalAmount = totalAmount;
            this.appliedCouponId = appliedCouponId;
        }
    }

    @Operation(
            summary = "주문 생성",
            description = "장바구니 기반으로 주문을 생성합니다. 쿠폰을 적용할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "주문 생성 성공",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (장바구니 비어있음, 쿠폰 오류 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "쿠폰 이미 사용됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderCreateRequestDto request) {

        // 실패 케이스 1: 장바구니가 비어있음
        if (request.getCartItems() == null || request.getCartItems().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("EMPTY_CART", "장바구니가 비어있습니다."));
        }

        // 실패 케이스 2: 수량이 0 이하인 항목이 있음
        for (OrderCreateRequestDto.CartItemInfo item : request.getCartItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_QUANTITY",
                                "수량은 1개 이상이어야 합니다. (cartItemId: " + item.getCartItemId() + ")"));
            }
        }

        // 쿠폰 처리
        Long appliedCouponId = null;
        Integer discountAmount = 0;

        if (request.getCouponId() != null) {
            // 실패 케이스 3: 존재하지 않는 쿠폰
            if (!COUPONS.containsKey(request.getCouponId())) {
                return ResponseEntity.status(404)
                        .body(new ErrorResponse("COUPON_NOT_FOUND",
                                "쿠폰을 찾을 수 없습니다. (ID: " + request.getCouponId() + ")"));
            }

            // 실패 케이스 4: 이미 사용된 쿠폰
            Set<Long> userUsedCoupons = USED_COUPONS.getOrDefault(request.getUserId(), new HashSet<>());
            if (userUsedCoupons.contains(request.getCouponId())) {
                return ResponseEntity.status(409)
                        .body(new ErrorResponse("COUPON_ALREADY_USED",
                                "이미 사용된 쿠폰입니다. (ID: " + request.getCouponId() + ")"));
            }

            appliedCouponId = request.getCouponId();
            discountAmount = COUPONS.get(request.getCouponId());

            // 쿠폰 사용 기록
            USED_COUPONS.putIfAbsent(request.getUserId(), new HashSet<>());
            USED_COUPONS.get(request.getUserId()).add(appliedCouponId);
        }

        // 성공 케이스: 주문 생성
        Long orderId = ORDER_ID_GENERATOR.getAndIncrement();

        // Mock 주문 금액 계산 (실제로는 상품 가격 * 수량)
        int totalAmount = request.getCartItems().stream()
                .mapToInt(item -> item.getQuantity() * 15000) // 평균 가격 15,000원으로 가정
                .sum();
        totalAmount = Math.max(0, totalAmount - discountAmount); // 할인 적용

        MockOrder order = new MockOrder(orderId, request.getUserId(), "CREATED", totalAmount, appliedCouponId);
        ORDERS.put(orderId, order);

        OrderResponseDto response = new OrderResponseDto(orderId, "CREATED", appliedCouponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "주문 결제",
            description = "생성된 주문을 결제 처리합니다. 잔액 확인 및 결제 수단 검증을 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = OrderPayResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 결제 수단, 잔액 부족 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 결제된 주문",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}")
    public ResponseEntity<?> payOrder(
            @Parameter(description = "주문 ID", example = "1001", required = true)
            @PathVariable Long id,
            @RequestBody OrderPayRequestDto request) {

        // 실패 케이스 1: 존재하지 않는 주문
        MockOrder order = ORDERS.get(id);
        if (order == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다. (ID: " + id + ")"));
        }

        // 실패 케이스 2: 이미 결제된 주문
        if ("PAID".equals(order.status)) {
            return ResponseEntity.status(409)
                    .body(new ErrorResponse("ORDER_ALREADY_PAID", "이미 결제된 주문입니다. (ID: " + id + ")"));
        }

        // 실패 케이스 3: 취소된 주문
        if ("CANCELLED".equals(order.status)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("ORDER_CANCELLED", "취소된 주문입니다. (ID: " + id + ")"));
        }

        // 실패 케이스 4: 잘못된 결제 수단
        List<String> validPaymentMethods = List.of("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "MOBILE_PAY");
        if (request.getPaymentMethod() == null || !validPaymentMethods.contains(request.getPaymentMethod())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_PAYMENT_METHOD",
                            "지원하지 않는 결제 수단입니다. (입력값: " + request.getPaymentMethod() +
                                    ", 지원: " + String.join(", ", validPaymentMethods) + ")"));
        }

        // 실패 케이스 5: 잔액 부족 (BANK_TRANSFER의 경우에만 체크)
        if ("BANK_TRANSFER".equals(request.getPaymentMethod())) {
            Integer balance = USER_BALANCE.getOrDefault(order.userId, 0);
            if (balance < order.totalAmount) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INSUFFICIENT_BALANCE",
                                "잔액이 부족합니다. (필요 금액: " + order.totalAmount + "원, 잔액: " + balance + "원)"));
            }
            // 잔액 차감
            USER_BALANCE.put(order.userId, balance - order.totalAmount);
        }

        // 성공 케이스: 결제 처리
        order.status = "PAID";
        order.paymentMethod = request.getPaymentMethod();

        OrderPayResponseDto response = new OrderPayResponseDto(id, "PAID");
        return ResponseEntity.ok(response);
    }
}