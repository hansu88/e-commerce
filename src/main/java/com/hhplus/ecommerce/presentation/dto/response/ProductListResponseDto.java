package com.hhplus.ecommerce.presentation.dto.response;

public record ProductListResponseDto(
    Long id,
    String name,
    Integer price,
    String status,
    Integer stock
) {}
