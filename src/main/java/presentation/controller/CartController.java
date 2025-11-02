package presentation.controller;

import presentation.dto.request.CartAddRequestDto;
import presentation.dto.response.CartItemResponseDto;
import presentation.dto.response.ErrorResponse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart", description = "장바구니 관리 API")
public class CartController {

    // Mock 데이터 저장소
    private static final AtomicLong CART_ID_GENERATOR = new AtomicLong(1);
    private static final Map<Long, List<CartItemResponseDto>> USER_CARTS = new ConcurrentHashMap<>();

    // 상품 옵션별 재고 (ProductController의 옵션 ID와 매칭)
    private static final Map<Long, Integer> PRODUCT_OPTION_STOCKS = new HashMap<>() {{
        put(1L, 50);  // RED, M
        put(2L, 30);  // BLUE, L
        put(3L, 0);   // BLACK, XL (품절)
        put(4L, 15);  // 청바지 BLUE, 28
        put(5L, 10);  // 청바지 BLUE, 30
        put(12L, 25); // 조거팬츠 BLACK, M
        put(13L, 20); // 조거팬츠 GRAY, L
    }};

    // 상품 옵션 정보
    private static final Map<Long, CartItemResponseDto.ProductOption> PRODUCT_OPTIONS = new HashMap<>() {{
        put(1L, new CartItemResponseDto.ProductOption(1L, "RED", "M"));
        put(2L, new CartItemResponseDto.ProductOption(2L, "BLUE", "L"));
        put(3L, new CartItemResponseDto.ProductOption(3L, "BLACK", "XL"));
        put(4L, new CartItemResponseDto.ProductOption(4L, "BLUE", "28"));
        put(5L, new CartItemResponseDto.ProductOption(5L, "BLUE", "30"));
        put(12L, new CartItemResponseDto.ProductOption(12L, "BLACK", "M"));
        put(13L, new CartItemResponseDto.ProductOption(13L, "GRAY", "L"));
    }};

    static {
        // 초기 Mock 데이터: userId 1번의 장바구니
        USER_CARTS.put(1L, new ArrayList<>(List.of(
                new CartItemResponseDto(1L, PRODUCT_OPTIONS.get(1L), 2),
                new CartItemResponseDto(2L, PRODUCT_OPTIONS.get(2L), 1)
        )));
    }

    @Operation(
            summary = "장바구니 담기",
            description = "상품 옵션을 장바구니에 추가합니다. 재고 확인 및 수량 검증을 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "장바구니 추가 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (수량 오류, 재고 부족 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "상품 옵션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> addCart(@RequestBody CartAddRequestDto request) {

        // 실패 케이스 1: 수량이 0 이하
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_QUANTITY", "수량은 1개 이상이어야 합니다. (입력값: " + request.getQuantity() + ")"));
        }

        // 실패 케이스 2: 수량이 너무 많음 (100개 제한)
        if (request.getQuantity() > 100) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("QUANTITY_EXCEEDED", "한 번에 최대 100개까지만 담을 수 있습니다. (입력값: " + request.getQuantity() + ")"));
        }

        // 실패 케이스 3: 존재하지 않는 상품 옵션
        if (!PRODUCT_OPTIONS.containsKey(request.getProductOptionId())) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("PRODUCT_OPTION_NOT_FOUND", "상품 옵션을 찾을 수 없습니다. (ID: " + request.getProductOptionId() + ")"));
        }

        // 실패 케이스 4: 재고 부족
        Integer stock = PRODUCT_OPTION_STOCKS.get(request.getProductOptionId());
        if (stock == null || stock < request.getQuantity()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("OUT_OF_STOCK",
                            "재고가 부족합니다. (요청 수량: " + request.getQuantity() + ", 재고: " + (stock != null ? stock : 0) + ")"));
        }

        // 성공 케이스: 장바구니에 추가
        Long cartItemId = CART_ID_GENERATOR.getAndIncrement();

        // 사용자 장바구니가 없으면 생성
        USER_CARTS.putIfAbsent(request.getUserId(), new ArrayList<>());

        // 장바구니에 아이템 추가
        CartItemResponseDto newItem = new CartItemResponseDto(
                cartItemId,
                PRODUCT_OPTIONS.get(request.getProductOptionId()),
                request.getQuantity()
        );
        USER_CARTS.get(request.getUserId()).add(newItem);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("cartItemId", cartItemId));
    }

    @Operation(
            summary = "장바구니 조회",
            description = "사용자의 장바구니에 담긴 상품 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CartItemResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "사용자 ID가 누락됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> getCart(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long uid) {

        // 실패 케이스: uid가 null (실제로는 @RequestParam이 처리하지만 명시적 검증)
        if (uid == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("MISSING_USER_ID", "사용자 ID는 필수입니다."));
        }

        // 성공 케이스: 장바구니 조회 (빈 장바구니도 성공)
        List<CartItemResponseDto> cartItems = USER_CARTS.getOrDefault(uid, new ArrayList<>());
        return ResponseEntity.ok(cartItems);
    }

    @Operation(
            summary = "장바구니 항목 삭제",
            description = "장바구니에서 특정 항목을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCartItem(
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long id) {

        // 모든 사용자의 장바구니에서 해당 항목 찾기
        boolean found = false;
        for (List<CartItemResponseDto> cartItems : USER_CARTS.values()) {
            boolean removed = cartItems.removeIf(item -> item.getCartItemId().equals(id));
            if (removed) {
                found = true;
                break;
            }
        }

        // 실패 케이스: 존재하지 않는 장바구니 항목
        if (!found) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("CART_ITEM_NOT_FOUND", "장바구니 항목을 찾을 수 없습니다. (ID: " + id + ")"));
        }

        // 성공 케이스: 삭제 완료
        return ResponseEntity.noContent().build();
    }
}
