package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전체 상품 목록 조회 UseCase (개선)
 * - Native Query + DTO 변환으로 N+1 문제 해결
 * - 기존: N+1 쿼리 (1 + 상품 수)
 * - 개선: 1개 쿼리 (JOIN + GROUP BY)
 */
@Component
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;

    /**
     * 상품 목록 조회 (캐싱 적용)
     *
     * @Cacheable:
     * - 캐시 이름: "productList" (TTL 30초)
     * - 캐시 키: 파라미터 없음 → SimpleKey[] (전체 목록 캐싱)
     * - 동작: 캐시 히트 시 DB 조회 생략, 미스 시 DB 조회 후 캐싱
     *
     * Master-Replica 동작:
     * 1. 캐시 미스 → Master에서 쓰기 → Replica로 복제
     * 2. 캐시 히트 → Replica에서 읽기 (부하 분산)
     *
     * TTL 30초 이유:
     * - 재고 변동이 자주 발생 → 짧은 TTL로 신선도 유지
     * - 그럼에도 30초간 DB 조회 없이 응답 → 성능 향상
     */
    @Cacheable(value = "productList")
    @Transactional(readOnly = true)
    public List<ProductListResponseDto> execute() {
        // Native Query로 1번의 쿼리만 실행
        List<Object[]> results = productRepository.findAllWithTotalStockNative();

        // Object[] -> DTO 변환
        return results.stream()
                .map(row -> new ProductListResponseDto(
                        ((Number) row[0]).longValue(),           // id
                        (String) row[1],                          // name
                        ((Number) row[2]).intValue(),             // price
                        (String) row[3],                          // status
                        ((Number) row[4]).intValue()              // total_stock
                ))
                .collect(Collectors.toList());
    }
}
