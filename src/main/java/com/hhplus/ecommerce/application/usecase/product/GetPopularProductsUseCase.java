package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.application.command.product.GetPopularProductsCommand;
import com.hhplus.ecommerce.domain.product.PopularProduct;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.infrastructure.persistence.base.PopularProductRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductPopularResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 인기 상품 조회 UseCase (개선)
 * - 집계 테이블(popular_products)을 사용한 고성능 조회
 * - 최근 N일간의 일별 집계 데이터를 합산하여 인기 상품 반환
 */
@Component
@RequiredArgsConstructor
public class GetPopularProductsUseCase {

    private final ProductRepository productRepository;
    private final PopularProductRepository popularProductRepository;

    /**
     * 인기 상품 조회 (캐싱 적용)
     *
     * @Cacheable:
     * - 캐시 이름: "popularProducts" (TTL 5분)
     * - 캐시 키: days + limit (예: "3::10")
     * - 동작: 캐시 히트 시 DB 조회 생략, 미스 시 DB 조회 후 캐싱
     *
     * Master-Replica 동작:
     * 1. 캐시 미스 → Master에서 쓰기 → Replica로 복제
     * 2. 캐시 히트 → Replica에서 읽기 (부하 분산)
     */
    @Cacheable(
            value = "popularProducts",
            key = "#command.days + '::' + #command.limit"
    )
    @Transactional(readOnly = true)
    public List<ProductPopularResponseDto> execute(GetPopularProductsCommand command) {
        // 1. 최근 N일간의 일별 집계 데이터 조회
        LocalDate startDate = LocalDate.now().minusDays(command.getDays());
        List<PopularProduct> recentPopularProducts = popularProductRepository
                .findRecentByPeriodType(PopularProduct.PeriodType.DAILY, startDate);

        // 2. 상품별 판매량 합산
        Map<Long, Integer> productSalesMap = new HashMap<>();
        for (PopularProduct pp : recentPopularProducts) {
            productSalesMap.put(
                    pp.getProductId(),
                    productSalesMap.getOrDefault(pp.getProductId(), 0) + pp.getSalesCount()
            );
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
