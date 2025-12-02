package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.application.command.product.GetPopularProductsCommand;
import com.hhplus.ecommerce.application.command.product.GetProductDetailCommand;
import com.hhplus.ecommerce.application.command.product.GetProductRankingCommand;
import com.hhplus.ecommerce.application.usecase.product.GetPopularProductsUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductDetailUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductListUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductRankingUseCase;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductPopularResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductRankingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * validate 관련 command클래스추가하여 Exception처리
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final GetProductListUseCase getProductListUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;
    private final GetProductRankingUseCase getProductRankingUseCase;

    /** 전체 상품 조회 */
    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getProducts() {
        return ResponseEntity.ok(getProductListUseCase.execute());
    }

    /** 상품 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponseDto> getProduct(@PathVariable Long id) {
        GetProductDetailCommand command = new GetProductDetailCommand(id);
        command.validate();
        return ResponseEntity.ok(getProductDetailUseCase.execute(command));
    }

    /** 인기 상품 조회 */
    @GetMapping("/popular")
    public ResponseEntity<List<ProductPopularResponseDto>> getPopularProducts(
            @RequestParam(defaultValue = "3") int days,
            @RequestParam(defaultValue = "5") int limit) {

        GetPopularProductsCommand command = new GetPopularProductsCommand(days, limit);
        command.validate();
        return ResponseEntity.ok(getPopularProductsUseCase.execute(command));
    }

    /**
     * 상품 랭킹 조회 (Redis Sorted Set)
     * - 실시간 판매 수량 기반 랭킹
     * - Redis에서 TOP N 조회
     *
     * @param count 조회할 개수 (기본값: 5)
     * @return 상품 랭킹 리스트
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<ProductRankingResponseDto>> getProductRanking(
            @RequestParam(defaultValue = "5") int count) {

        GetProductRankingCommand command = new GetProductRankingCommand(count);
        command.validate();
        return ResponseEntity.ok(getProductRankingUseCase.execute(command));
    }
}