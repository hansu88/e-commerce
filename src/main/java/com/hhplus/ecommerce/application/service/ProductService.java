package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.exception.ProductNotFoundException;
import com.hhplus.ecommerce.presentation.dto.response.ProductDetailResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.ProductPopularResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상품 서비스
 * - 상품 조회, 상세 조회, 인기 상품 조회 처리
 */
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
     * - Product와 ProductOption을 결합하여 ProductDetailResponseDto 생성
     * - 옵션별 재고 정보 포함
     *
     * @param productId 조회할 상품 ID
     * @return 상품 상세 DTO
     * @throws ProductNotFoundException 상품이 존재하지 않을 경우
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
     * 인기 상품 조회 (최근 N일간 판매량 기준)
     * @param days 최근 일수
     * @param limit 조회할 상품 개수
     * @return 인기 상품 목록
     */
    public List<ProductPopularResponseDto> getPopularProductsDto(int days, int limit) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // 최근 N일간의 모든 OrderItem 조회
        List<OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());
        
        // ProductOption ID → Product ID 매핑
        Map<Long, Long> optionToProductMap = new HashMap<>();
        for (ProductOption option : productOptionRepository.findAll()) {
            optionToProductMap.put(option.getId(), option.getProductId());
        }
        
        // Product별 판매량 집계
        Map<Long, Integer> productSalesMap = new HashMap<>();
        for (OrderItem item : recentOrderItems) {
            Long productId = optionToProductMap.get(item.getProductOptionId());
            if (productId != null) {
                productSalesMap.merge(productId, item.getQuantity(), Integer::sum);
            }
        }
        
        // Product 정보 조회 및 판매량과 결합
        return productRepository.findAll().stream()
                .filter(product -> productSalesMap.containsKey(product.getId()))
                .map(product -> new ProductPopularResponseDto(
                        product.getId(),
                        product.getName(),
                        productSalesMap.get(product.getId())
                ))
                .sorted((a, b) -> Integer.compare(b.getSoldCount(), a.getSoldCount())) // 판매량 내림차순
                .limit(limit)
                .collect(Collectors.toList());
    }
}
