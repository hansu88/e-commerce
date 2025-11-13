package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 전체 상품 목록 조회 UseCase (개선)
 * - DTO 직접 조회로 N+1 문제 해결
 * - 기존: N+1 쿼리 (1 + 상품 수)
 * - 개선: 1개 쿼리 (JOIN + GROUP BY)
 */
@Component
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductListResponseDto> execute() {
        // DTO 직접 조회로 1번의 쿼리만 실행
        return productRepository.findAllWithTotalStock();
    }
}
