package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GetProductListUseCaseTest {

    @Autowired
    private GetProductListUseCase getProductListUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @BeforeEach
    void setUp() {
        productOptionRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("상품 목록 조회 - DTO 직접 조회로 N+1 해결")
    void getProductList_Success() {
        // Given
        Product product1 = createProduct("상품 A", 10000);
        createProductOption(product1.getId(), "Red", "M", 50);
        createProductOption(product1.getId(), "Blue", "L", 30);

        Product product2 = createProduct("상품 B", 20000);
        createProductOption(product2.getId(), "Black", "XL", 100);

        Product product3 = createProduct("상품 C (옵션 없음)", 30000);
        // 옵션 없음

        // When
        List<ProductListResponseDto> results = getProductListUseCase.execute();

        // Then
        assertThat(results).hasSize(3);

        // 상품 A: 재고 합계 80 (50 + 30)
        ProductListResponseDto productA = results.stream()
                .filter(p -> p.id().equals(product1.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(productA.name()).isEqualTo("상품 A");
        assertThat(productA.price()).isEqualTo(10000);
        assertThat(productA.totalStock()).isEqualTo(80);

        // 상품 B: 재고 합계 100
        ProductListResponseDto productB = results.stream()
                .filter(p -> p.id().equals(product2.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(productB.totalStock()).isEqualTo(100);

        // 상품 C: 재고 0 (옵션 없음)
        ProductListResponseDto productC = results.stream()
                .filter(p -> p.id().equals(product3.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(productC.totalStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 목록 조회 - 빈 결과")
    void getProductList_Empty() {
        // When
        List<ProductListResponseDto> results = getProductListUseCase.execute();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("상품 목록 조회 - 재고 합계 정확성 검증")
    void getProductList_StockSum() {
        // Given
        Product product = createProduct("테스트 상품", 10000);
        createProductOption(product.getId(), "Red", "S", 10);
        createProductOption(product.getId(), "Red", "M", 20);
        createProductOption(product.getId(), "Red", "L", 30);
        createProductOption(product.getId(), "Blue", "S", 5);

        // When
        List<ProductListResponseDto> results = getProductListUseCase.execute();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).totalStock()).isEqualTo(65); // 10+20+30+5
    }

    // === Helper Methods ===

    private Product createProduct(String name, int price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    private ProductOption createProductOption(Long productId, String color, String size, int stock) {
        ProductOption option = new ProductOption();
        option.setProductId(productId);
        option.setColor(color);
        option.setSize(size);
        option.setStock(stock);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        return productOptionRepository.save(option);
    }
}
