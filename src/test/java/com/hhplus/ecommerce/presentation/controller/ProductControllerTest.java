package com.hhplus.ecommerce.presentation.controller;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Test
    @DisplayName("GET /api/products - 상품 목록 조회")
    void getProducts() throws Exception {
        Product product = new Product();
        product.setName("테스트 상품");
        product.setPrice(10000);
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/products/{id} - 상품 상세 조회")
    void getProduct() throws Exception {
        Product product = new Product();
        product.setName("테스트 상품");
        product.setPrice(10000);
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);

        ProductOption option = new ProductOption();
        option.setProductId(saved.getId());
        option.setColor("Black");
        option.setSize("M");
        option.setStock(100);
        productOptionRepository.save(option);

        mockMvc.perform(get("/api/products/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    @DisplayName("GET /api/products/popular - 인기 상품 조회")
    void getPopularProducts() throws Exception {
        mockMvc.perform(get("/api/products/popular")
                        .param("days", "3")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
