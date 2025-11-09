package com.hhplus.ecommerce.presentation.dto.response;

public record ProductPopularResponseDto(
    Long id,
    String name,
    Integer soldCount
) {}