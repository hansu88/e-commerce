package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import lombok.RequiredArgsConstructor;
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
