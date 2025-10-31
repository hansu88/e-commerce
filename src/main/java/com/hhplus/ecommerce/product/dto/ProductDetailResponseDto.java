package com.hhplus.ecommerce.product.dto;

import java.util.List;

public class ProductDetailResponseDto {
    private Long id;
    private String name;
    private Integer price;
    private String status;
    private List<ProductOptionDto> options;

    public static class ProductOptionDto {
        private Long id;
        private String color;
        private String size;
        private Integer stock;
    }
}