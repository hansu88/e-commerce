package com.hhplus.ecommerce.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDetailResponseDto {
    private Long id;
    private String name;
    private Integer price;
    private String status;
    private List<ProductOptionDto> options;

    @Getter
    @AllArgsConstructor
    public static class ProductOptionDto {
        private Long id;
        private String color;
        private String size;
        private Integer stock;
    }
}