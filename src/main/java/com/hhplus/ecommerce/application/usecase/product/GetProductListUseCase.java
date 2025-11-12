package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전체 상품 목록 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    @Transactional(readOnly = true)
    public List<ProductListResponseDto> execute() {
        return productRepository.findAll().stream()
                .map(product -> {
                    // 각 상품의 옵션 재고 합계 계산
                    int totalStock = productOptionRepository.findByProductId(product.getId())
                            .stream()
                            .mapToInt(ProductOption::getStock)
                            .sum();

                    return new ProductListResponseDto(
                            product.getId(),
                            product.getName(),
                            product.getPrice(),
                            product.getStatus().name(),
                            totalStock
                    );
                })
                .collect(Collectors.toList());
    }
}
