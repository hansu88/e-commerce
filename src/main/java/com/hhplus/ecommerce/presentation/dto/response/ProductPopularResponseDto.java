package com.hhplus.ecommerce.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductPopularResponseDto {
    private Long id;
    private String name;
    private Integer soldCount;
}