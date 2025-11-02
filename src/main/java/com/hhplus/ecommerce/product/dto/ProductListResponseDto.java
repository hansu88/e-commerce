package com.hhplus.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductListResponseDto {
    private Long id;
    private String name;
    private Integer price;
    private String status;
    private Integer stock;
}
