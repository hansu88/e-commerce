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
        // Mock data
        return List.of(new ProductListResponseDto());
    }

    @GetMapping("/{id}")
    public ProductDetailResponseDto getProduct(@PathVariable Long id) {
        // Mock data
        return new ProductDetailResponseDto();
    }

    @GetMapping("/popular")
    public List<ProductPopularResponseDto> getPopularProducts(
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "5") Integer limit) {
        return List.of(new ProductPopularResponseDto());
    }
}