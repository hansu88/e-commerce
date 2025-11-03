package com.hhplus.ecommerce.domain.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상품 옵션 Entity
 */
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductOption {
    private Long id;
    private Long productId;
    private String color;
    private String size;
    private Integer stock;

}
