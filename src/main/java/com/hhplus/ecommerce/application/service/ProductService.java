package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductOptionRepository;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductPopularResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public ProductService(ProductRepository productRepository,
                          ProductOptionRepository productOptionRepository) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
    }

    /**
     * 전체 상품 조회 (DTO 반환)
     */
    public List<ProductListResponseDto> getProductsDto() {
        return productRepository.findAll().stream()
                .map(product -> new ProductListResponseDto(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStatus().name(),
                        0 // stock 합계 계산 (추후 구현)
                ))
                .collect(Collectors.toList());
    }

    /**
     * 상품 상세 조회 (옵션 포함, DTO 반환)
     */
    public ProductDetailResponseDto getProductDetailDto(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductOption> options = productOptionRepository.findByProductId(productId);

        List<ProductDetailResponseDto.ProductOptionDto> optionDtos = options.stream()
                .map(option -> ProductDetailResponseDto.ProductOptionDto.builder()
                        .id(option.getId())
                        .color(option.getColor())
                        .size(option.getSize())
                        .stock(option.getStock())
                        .build())
                .collect(Collectors.toList());

        return ProductDetailResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .options(optionDtos)
                .build();
    }

    /**
     * 인기 상품 조회 (DTO 반환)
     */
    public List<ProductPopularResponseDto> getPopularProductsDto(int days, int limit) {
        // TODO: OrderItem 집계 로직 적용
        return productRepository.findAll().stream()
                .limit(limit)
                .map(product -> new ProductPopularResponseDto(
                        product.getId(),
                        product.getName(),
                        0 // soldCount 추후 계산
                ))
                .collect(Collectors.toList());
    }
}