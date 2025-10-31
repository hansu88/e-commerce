package com.hhplus.ecommerce.controller;


import com.hhplus.ecommerce.product.dto.ProductDetailResponseDto;
import com.hhplus.ecommerce.product.dto.ProductListResponseDto;
import com.hhplus.ecommerce.product.dto.ProductPopularResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<ProductListResponseDto> getProducts() {
        return List.of(
                new ProductListResponseDto(1L, "기본 티셔츠", 29000, "ACTIVE", 50),
                new ProductListResponseDto(2L, "청바지", 59000, "ACTIVE", 30)
        );
    }

    @GetMapping("/{id}")
    public ProductDetailResponseDto getProduct(@PathVariable Long id) {
        return new ProductDetailResponseDto(
                id,
                "기본 티셔츠",
                29000,
                "ACTIVE",
                List.of(new ProductDetailResponseDto.ProductOptionDto(1L, "RED", "M", 50))
        );
    }

    @GetMapping("/popular")
    public List<ProductPopularResponseDto> getPopularProducts(
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "5") Integer limit) {
        return List.of(
                new ProductPopularResponseDto(1L, "기본 티셔츠", 10),
                new ProductPopularResponseDto(2L, "청바지", 8)
        );
    }
}