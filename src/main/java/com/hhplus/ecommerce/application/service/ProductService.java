package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.exception.ProductNotFoundException;
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
    private final OrderItemRepository orderItemRepository;

    /**
     * 전체 상품 조회 (DTO 반환)
     * - Product → ProductListResponseDto 변환
     *
     * @return 전체 상품 목록 DTO
     */
    public ProductService(ProductRepository productRepository,
                          ProductOptionRepository productOptionRepository,
                          OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.orderItemRepository = orderItemRepository;
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
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

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
     * 최근 N일간 판매량 기준 상위 상품 조회
     */
    public List<ProductPopularResponseDto> getPopularProductsDto(int days, int limit) {
        // 1. 최근 N일간의 OrderItem 조회
        java.time.LocalDateTime startDate = java.time.LocalDateTime.now().minusDays(days);
        List<com.hhplus.ecommerce.domain.order.OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        // 2. ProductOption → Product 매핑하여 상품별 판매량 집계
        java.util.Map<Long, Integer> productSalesMap = new java.util.HashMap<>();

        for (com.hhplus.ecommerce.domain.order.OrderItem item : recentOrderItems) {
            ProductOption option = productOptionRepository.findById(item.getProductOptionId()).orElse(null);
            if (option != null) {
                Long productId = option.getProductId();
                productSalesMap.put(productId,
                    productSalesMap.getOrDefault(productId, 0) + item.getQuantity());
            }
        }

        // 3. 판매량 순으로 정렬 후 상위 limit개 반환
        return productSalesMap.entrySet().stream()
                .sorted(java.util.Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Product product = productRepository.findById(entry.getKey()).orElse(null);
                    if (product != null) {
                        return new ProductPopularResponseDto(
                                product.getId(),
                                product.getName(),
                                entry.getValue()
                        );
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
