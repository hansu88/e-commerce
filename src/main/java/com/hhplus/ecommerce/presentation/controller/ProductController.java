package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.service.ProductService;
import com.hhplus.ecommerce.presentation.dto.response.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductPopularResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "전체 상품 조회")
    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getProducts() {
        return ResponseEntity.ok(productService.getProductsDto());
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponseDto> getProduct(@PathVariable Long id) {
        ProductDetailResponseDto response = productService.getProductDetailDto(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인기 상품 조회")
    @GetMapping("/popular")
    public ResponseEntity<List<ProductPopularResponseDto>> getPopularProducts(
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "5") Integer limit) {

        if (days <= 0 || days > 30) {
            throw new IllegalArgumentException("조회 기간은 1~30 사이여야 합니다.");
        }

        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("조회 개수는 1~100 사이여야 합니다.");
        }

        List<ProductPopularResponseDto> response = productService.getPopularProductsDto(days, limit);
        return ResponseEntity.ok(response);
    }
}
