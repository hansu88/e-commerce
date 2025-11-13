package com.hhplus.ecommerce.presentation.dto.response.product;

public record ProductListResponseDto(
        Long id,
        String name,
        Integer price,
        String status,
        Integer totalStock // 기존 stock → totalStock으로 변경
) {}