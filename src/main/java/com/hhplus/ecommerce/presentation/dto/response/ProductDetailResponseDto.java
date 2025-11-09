package com.hhplus.ecommerce.presentation.dto.response;

import java.util.List;

public record ProductDetailResponseDto(
    Long id,
    String name,
    Integer price,
    String status,
    List<ProductOptionDto> options
) {
    public record ProductOptionDto(
        Long id,
        String color,
        String size,
        Integer stock
    ) {}
}