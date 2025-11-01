package com.hhplus.ecommerce.controller;

import com.hhplus.ecommerce.common.dto.ErrorResponse;
import com.hhplus.ecommerce.product.dto.ProductDetailResponseDto;
import com.hhplus.ecommerce.product.dto.ProductListResponseDto;
import com.hhplus.ecommerce.product.dto.ProductPopularResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    // Mock 데이터 저장소 - 상품 목록
    private static final List<ProductListResponseDto> MOCK_PRODUCTS = List.of(
            new ProductListResponseDto(1L, "기본 티셔츠", 29000, "ACTIVE", 50),
            new ProductListResponseDto(2L, "청바지", 59000, "ACTIVE", 30),
            new ProductListResponseDto(3L, "후드티", 45000, "ACTIVE", 20),
            new ProductListResponseDto(4L, "맨투맨", 39000, "ACTIVE", 15),
            new ProductListResponseDto(5L, "조거팬츠", 35000, "ACTIVE", 25),
            new ProductListResponseDto(999L, "품절 상품", 10000, "SOLD_OUT", 0)
    );

    // Mock 데이터 저장소 - 상품 상세 (ID별)
    private static final Map<Long, ProductDetailResponseDto> MOCK_PRODUCT_DETAILS = new HashMap<>() {{
        put(1L, new ProductDetailResponseDto(1L, "기본 티셔츠", 29000, "ACTIVE", List.of(
                new ProductDetailResponseDto.ProductOptionDto(1L, "RED", "M", 50),
                new ProductDetailResponseDto.ProductOptionDto(2L, "BLUE", "L", 30),
                new ProductDetailResponseDto.ProductOptionDto(3L, "BLACK", "XL", 0)
        )));
        put(2L, new ProductDetailResponseDto(2L, "청바지", 59000, "ACTIVE", List.of(
                new ProductDetailResponseDto.ProductOptionDto(4L, "BLUE", "28", 15),
                new ProductDetailResponseDto.ProductOptionDto(5L, "BLUE", "30", 10),
                new ProductDetailResponseDto.ProductOptionDto(6L, "BLACK", "32", 5)
        )));
        put(3L, new ProductDetailResponseDto(3L, "후드티", 45000, "ACTIVE", List.of(
                new ProductDetailResponseDto.ProductOptionDto(7L, "GRAY", "M", 20),
                new ProductDetailResponseDto.ProductOptionDto(8L, "NAVY", "L", 15),
                new ProductDetailResponseDto.ProductOptionDto(9L, "BLACK", "XL", 10)
        )));
        put(4L, new ProductDetailResponseDto(4L, "맨투맨", 39000, "ACTIVE", List.of(
                new ProductDetailResponseDto.ProductOptionDto(10L, "WHITE", "M", 8),
                new ProductDetailResponseDto.ProductOptionDto(11L, "BEIGE", "L", 7)
        )));
        put(5L, new ProductDetailResponseDto(5L, "조거팬츠", 35000, "ACTIVE", List.of(
                new ProductDetailResponseDto.ProductOptionDto(12L, "BLACK", "M", 25),
                new ProductDetailResponseDto.ProductOptionDto(13L, "GRAY", "L", 20)
        )));
        put(999L, new ProductDetailResponseDto(999L, "품절 상품", 10000, "SOLD_OUT", List.of(
                new ProductDetailResponseDto.ProductOptionDto(14L, "RED", "M", 0)
        )));
    }};

    // Mock 데이터 저장소 - 날짜별 인기 상품 순위
    private static final Map<Integer, List<ProductPopularResponseDto>> MOCK_POPULAR_BY_DAYS = new HashMap<>() {{
        // 최근 1일 인기 상품 (신상품이 인기)
        put(1, List.of(
                new ProductPopularResponseDto(5L, "조거팬츠", 85),
                new ProductPopularResponseDto(3L, "후드티", 72),
                new ProductPopularResponseDto(1L, "기본 티셔츠", 65),
                new ProductPopularResponseDto(4L, "맨투맨", 48),
                new ProductPopularResponseDto(2L, "청바지", 35)
        ));
        // 최근 3일 인기 상품 (기본 상품이 누적으로 많음)
        put(3, List.of(
                new ProductPopularResponseDto(1L, "기본 티셔츠", 120),
                new ProductPopularResponseDto(2L, "청바지", 98),
                new ProductPopularResponseDto(3L, "후드티", 85),
                new ProductPopularResponseDto(4L, "맨투맨", 72),
                new ProductPopularResponseDto(5L, "조거팬츠", 65)
        ));
        // 최근 7일 인기 상품 (장기적으로는 청바지가 1위)
        put(7, List.of(
                new ProductPopularResponseDto(2L, "청바지", 245),
                new ProductPopularResponseDto(1L, "기본 티셔츠", 230),
                new ProductPopularResponseDto(3L, "후드티", 165),
                new ProductPopularResponseDto(5L, "조거팬츠", 142),
                new ProductPopularResponseDto(4L, "맨투맨", 128)
        ));
        // 최근 30일 인기 상품 (한 달 누적)
        put(30, List.of(
                new ProductPopularResponseDto(1L, "기본 티셔츠", 980),
                new ProductPopularResponseDto(2L, "청바지", 856),
                new ProductPopularResponseDto(3L, "후드티", 723),
                new ProductPopularResponseDto(5L, "조거팬츠", 645),
                new ProductPopularResponseDto(4L, "맨투맨", 512)
        ));
    }};

    @Operation(summary = "상품 목록 조회", description = "판매 중인 모든 상품 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductListResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getProducts() {
        return ResponseEntity.ok(MOCK_PRODUCTS);
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회합니다. 색상, 사이즈, 재고 등의 옵션 정보를 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long id) {

        // Mock 데이터에서 상품 찾기
        ProductDetailResponseDto product = MOCK_PRODUCT_DETAILS.get(id);

        // 실패 케이스: 존재하지 않는 상품
        if (product == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다. (ID: " + id + ")"));
        }

        // 성공 케이스
        return ResponseEntity.ok(product);
    }

    @Operation(
            summary = "인기 상품 조회",
            description = "지정된 기간 동안 판매량이 많은 상품을 조회합니다. " +
                    "기간에 따라 순위가 달라집니다. (1일, 3일, 7일, 30일 지원)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductPopularResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularProducts(
            @Parameter(description = "조회 기간 (일) - 1, 3, 7, 30일 중 선택", example = "3")
            @RequestParam(defaultValue = "3") Integer days,
            @Parameter(description = "조회 개수", example = "5")
            @RequestParam(defaultValue = "5") Integer limit) {

        // 실패 케이스: 잘못된 파라미터
        if (days <= 0 || days > 30) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_DAYS", "조회 기간은 1~30일 사이여야 합니다. (입력값: " + days + ")"));
        }

        if (limit <= 0 || limit > 100) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_LIMIT", "조회 개수는 1~100 사이여야 합니다. (입력값: " + limit + ")"));
        }

        // 성공 케이스: days에 가장 가까운 Mock 데이터 찾기
        List<ProductPopularResponseDto> popularProducts;
        if (days <= 2) {
            popularProducts = MOCK_POPULAR_BY_DAYS.get(1);
        } else if (days <= 5) {
            popularProducts = MOCK_POPULAR_BY_DAYS.get(3);
        } else if (days <= 15) {
            popularProducts = MOCK_POPULAR_BY_DAYS.get(7);
        } else {
            popularProducts = MOCK_POPULAR_BY_DAYS.get(30);
        }

        // limit만큼만 반환
        List<ProductPopularResponseDto> result = popularProducts.stream()
                .limit(limit)
                .toList();

        return ResponseEntity.ok(result);
    }
}
