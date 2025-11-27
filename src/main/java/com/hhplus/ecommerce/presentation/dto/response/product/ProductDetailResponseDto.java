package com.hhplus.ecommerce.presentation.dto.response.product;

import java.util.List;

public record ProductDetailResponseDto(
        Long id,
        String name,
        Integer price,
        String status,
        Integer totalStock, // 전체 재고 합계 추가
        List<ProductOptionDto> options
) {
    public record ProductOptionDto(
            Long id,
            String color,
            String size,
            Integer stock
    ) {}
}