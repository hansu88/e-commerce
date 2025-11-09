package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.application.command.GetPopularProductsCommand;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.ProductPopularResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인기 상품 조회 UseCase
 * - 최근 N일간 판매량 기준 상위 상품 조회
 */
@Component
@RequiredArgsConstructor
public class GetPopularProductsUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderItemRepository orderItemRepository;

    public List<ProductPopularResponseDto> execute(GetPopularProductsCommand command) {
        // 1. 최근 N일간의 OrderItem 조회
        LocalDateTime startDate = LocalDateTime.now().minusDays(command.getDays());
        List<com.hhplus.ecommerce.domain.order.OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        // 2. ProductOption → Product 매핑하여 상품별 판매량 집계
        Map<Long, Integer> productSalesMap = new HashMap<>();

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
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(command.getLimit())
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
