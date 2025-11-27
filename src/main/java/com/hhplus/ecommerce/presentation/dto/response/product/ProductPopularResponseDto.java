package com.hhplus.ecommerce.presentation.dto.response.product;

public record ProductPopularResponseDto(
    Long id,
    String name,
    Integer soldCount
) {}