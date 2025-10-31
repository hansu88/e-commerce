package com.hhplus.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductPopularResponseDto {
    private Long id;
    private String name;
    private Integer soldCount;
}