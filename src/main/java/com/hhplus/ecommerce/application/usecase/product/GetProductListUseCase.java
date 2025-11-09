package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.ProductListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전체 상품 목록 조회 UseCase
 */
@Component
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;

    public List<ProductListResponseDto> execute() {
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
}
