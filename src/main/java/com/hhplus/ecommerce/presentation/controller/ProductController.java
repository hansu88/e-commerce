package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.GetPopularProductsCommand;
import com.hhplus.ecommerce.application.command.GetProductDetailCommand;
import com.hhplus.ecommerce.application.usecase.product.GetPopularProductsUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductDetailUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductListUseCase;
import com.hhplus.ecommerce.presentation.dto.response.ErrorResponse;
import com.hhplus.ecommerce.presentation.dto.response.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductPopularResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final GetProductListUseCase getProductListUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;

    /** 전체 상품 조회 */
    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getProducts() {
        return ResponseEntity.ok(getProductListUseCase.execute());
    }

    /** 상품 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        GetProductDetailCommand command = new GetProductDetailCommand(id);
        ProductDetailResponseDto response = getProductDetailUseCase.execute(command);
        return ResponseEntity.ok(response);

    }

    /** 인기 상품 조회 */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularProducts(
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "5") Integer limit) {

        if (days <= 0 || days > 30) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_DAYS", "조회 기간은 1~30 사이여야 합니다."));
        }

        if (limit <= 0 || limit > 100) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_LIMIT", "조회 개수는 1~100 사이여야 합니다."));
        }

        GetPopularProductsCommand command = new GetPopularProductsCommand(days, limit);
        List<ProductPopularResponseDto> response = getPopularProductsUseCase.execute(command);
        return ResponseEntity.ok(response);
    }
}