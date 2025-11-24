package com.hhplus.ecommerce.application.usecase.product;

import com.hhplus.ecommerce.application.command.product.GetPopularProductsCommand;
import com.hhplus.ecommerce.domain.product.PopularProduct;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.PopularProductRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductPopularResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GetPopularProductsUseCaseTest {

    @Autowired
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PopularProductRepository popularProductRepository;

    @BeforeEach
    void setUp() {
        popularProductRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("인기 상품 조회 - 최근 N일간 집계 데이터 조회")
    void getPopularProducts_Success() {
        // Given
        Product product1 = createProduct("상품 A", 10000);
        Product product2 = createProduct("상품 B", 20000);
        Product product3 = createProduct("상품 C", 30000);

        LocalDate today = LocalDate.now();

        // 3일간의 집계 데이터 생성
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 10, today.minusDays(2));
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 15, today.minusDays(1));
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 20, today);

        createPopularProduct(product2.getId(), PopularProduct.PeriodType.DAILY, 30, today.minusDays(2));
        createPopularProduct(product2.getId(), PopularProduct.PeriodType.DAILY, 25, today.minusDays(1));
        createPopularProduct(product2.getId(), PopularProduct.PeriodType.DAILY, 20, today);

        createPopularProduct(product3.getId(), PopularProduct.PeriodType.DAILY, 5, today);

        // When
        GetPopularProductsCommand command = new GetPopularProductsCommand(3, 5);
        List<ProductPopularResponseDto> results = getPopularProductsUseCase.execute(command);

        // Then
        assertThat(results).hasSize(3);

        // 판매량 합계 순 정렬 확인
        // 상품 B: 30 + 25 + 20 = 75
        // 상품 A: 10 + 15 + 20 = 45
        // 상품 C: 5
        assertThat(results.get(0).id()).isEqualTo(product2.getId());
        assertThat(results.get(0).soldCount()).isEqualTo(75);

        assertThat(results.get(1).id()).isEqualTo(product1.getId());
        assertThat(results.get(1).soldCount()).isEqualTo(45);

        assertThat(results.get(2).id()).isEqualTo(product3.getId());
        assertThat(results.get(2).soldCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("인기 상품 조회 - limit 제한 적용")
    void getPopularProducts_WithLimit() {
        // Given
        Product product1 = createProduct("상품 A", 10000);
        Product product2 = createProduct("상품 B", 20000);
        Product product3 = createProduct("상품 C", 30000);

        LocalDate today = LocalDate.now();

        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 50, today);
        createPopularProduct(product2.getId(), PopularProduct.PeriodType.DAILY, 30, today);
        createPopularProduct(product3.getId(), PopularProduct.PeriodType.DAILY, 10, today);

        // When - limit: 2
        GetPopularProductsCommand command = new GetPopularProductsCommand(1, 2);
        List<ProductPopularResponseDto> results = getPopularProductsUseCase.execute(command);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(product1.getId());
        assertThat(results.get(1).id()).isEqualTo(product2.getId());
    }

    @Test
    @DisplayName("인기 상품 조회 - 집계 데이터 없을 때 빈 리스트 반환")
    void getPopularProducts_NoData() {
        // Given
        createProduct("상품 A", 10000);

        // When
        GetPopularProductsCommand command = new GetPopularProductsCommand(3, 5);
        List<ProductPopularResponseDto> results = getPopularProductsUseCase.execute(command);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("인기 상품 조회 - 오래된 집계 데이터는 제외")
    void getPopularProducts_ExcludeOldData() {
        // Given
        Product product1 = createProduct("상품 A", 10000);

        LocalDate today = LocalDate.now();

        // 최근 3일 데이터
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 10, today.minusDays(2));
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 15, today.minusDays(1));

        // 4일 전 데이터 (제외되어야 함)
        createPopularProduct(product1.getId(), PopularProduct.PeriodType.DAILY, 100, today.minusDays(4));

        // When - 최근 3일만 조회
        GetPopularProductsCommand command = new GetPopularProductsCommand(3, 5);
        List<ProductPopularResponseDto> results = getPopularProductsUseCase.execute(command);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).soldCount()).isEqualTo(25); // 10 + 15 (100은 제외)
    }

    // === Helper Methods ===

    private Product createProduct(String name, int price) {
        Product product = new Product(name, price, ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    private void createPopularProduct(Long productId, PopularProduct.PeriodType periodType,
                                      int salesCount, LocalDate aggregatedDate) {
        PopularProduct popularProduct = new PopularProduct(productId, periodType, salesCount, aggregatedDate);
        popularProductRepository.save(popularProduct);
    }
}
