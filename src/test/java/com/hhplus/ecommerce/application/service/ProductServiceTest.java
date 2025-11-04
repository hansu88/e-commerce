package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.ProductOptionRepository;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.exception.ProductNotFoundException;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ProductServiceTest {

    private ProductService productService;
    private ProductRepository productRepository;
    private ProductOptionRepository productOptionRepository;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productOptionRepository = new InMemoryProductOptionRepository();
        productService = new ProductService(productRepository, productOptionRepository);
    }

    @Test
    @DisplayName("상품 상세 조회 시 존재하지 않으면 ProductNotFoudException 발생 ")
    void getProductDetail_NotFound_Exception() {

        // Given - 존재하지 않는 상품 ID
        Long noProductId = 999L;

        // When & Then - 서비스 호출 시 발생
        assertThatThrownBy(() -> productService.getProductDetailDto(noProductId))
                .as("상품이 존재하지 않으면 ProductNotFoundException이 발생해야 한다")
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
