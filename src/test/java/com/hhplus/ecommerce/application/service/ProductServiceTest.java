package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.presentation.exception.ProductNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;


    

    @Test
    @Transactional
    @DisplayName("상품 상세 조회 시 존재하지 않으면 ProductNotFoudException 발생 ")
    void getProductDetailNotFoundExceptio() {

        // Given - 존재하지 않는 상품 ID
        Long noProductId = 999L;

        // When & Then - 서비스 호출 시 발생
        assertThatThrownBy(() -> productService.getProductDetailDto(noProductId))
                .as("상품이 존재하지 않으면 ProductNotFoundException이 발생해야 한다")
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
