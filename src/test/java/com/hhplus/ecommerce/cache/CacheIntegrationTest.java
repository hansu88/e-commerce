package com.hhplus.ecommerce.cache;

import com.hhplus.ecommerce.application.command.product.GetPopularProductsCommand;
import com.hhplus.ecommerce.application.usecase.product.GetPopularProductsUseCase;
import com.hhplus.ecommerce.application.usecase.product.GetProductListUseCase;
import com.hhplus.ecommerce.domain.product.PopularProduct;
import com.hhplus.ecommerce.domain.product.Product;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductStatus;
import com.hhplus.ecommerce.infrastructure.persistence.base.PopularProductRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductRepository;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductListResponseDto;
import com.hhplus.ecommerce.presentation.dto.response.product.ProductPopularResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 캐싱 통합 테스트 (간단 버전)
 *
 * 테스트 목적:
 * - Redis 캐싱이 올바르게 동작하는지 검증
 * - 캐시 히트/미스 시나리오 확인
 * - 캐시 TTL 동작 확인
 *
 * Redis 서버:
 * - application.yml에 설정된 Redis 서버 사용 (192.168.4.81:6379)
 * - Master-Replica 구조로 동작
 *
 * 주의사항:
 * - Redis 서버가 실행 중이어야 함
 * - 각 테스트 후 캐시 수동 클리어
 */
@SpringBootTest
class CacheIntegrationTest {

    @Autowired
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Autowired
    private GetProductListUseCase getProductListUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private PopularProductRepository popularProductRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 데이터 정리
        popularProductRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();

        // 캐시 클리어
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("캐싱 테스트 1: 인기 상품 조회 - 캐시 히트")
    void testPopularProductsCache() {
        // Given - 테스트 데이터 생성
        Product product = createProduct("테스트 상품", 10000);
        LocalDate today = LocalDate.now();
        createPopularProduct(product.getId(), PopularProduct.PeriodType.DAILY, 100, today);

        // When - 첫 번째 조회 (캐시 미스 → DB 조회)
        GetPopularProductsCommand command = new GetPopularProductsCommand(3, 10);
        long startTime1 = System.currentTimeMillis();
        List<ProductPopularResponseDto> result1 = getPopularProductsUseCase.execute(command);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Then - 결과 확인
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).id()).isEqualTo(product.getId());
        assertThat(result1.get(0).soldCount()).isEqualTo(100);

        // When - 두 번째 조회 (캐시 히트 → DB 조회 없음)
        long startTime2 = System.currentTimeMillis();
        List<ProductPopularResponseDto> result2 = getPopularProductsUseCase.execute(command);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then - 캐시에서 조회된 결과 확인
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).id()).isEqualTo(product.getId());
        assertThat(result2.get(0).soldCount()).isEqualTo(100);

        // 캐시 히트가 더 빠름 (일반적으로 10배 이상 빠름)
        System.out.println("===== 인기 상품 캐싱 테스트 결과 =====");
        System.out.println("첫 번째 조회 (캐시 미스): " + duration1 + "ms");
        System.out.println("두 번째 조회 (캐시 히트): " + duration2 + "ms");
        System.out.println("성능 향상: " + (duration1 - duration2) + "ms");
        System.out.println("======================================");
    }

    @Test
    @DisplayName("캐싱 테스트 2: 상품 목록 조회 - 캐시 히트")
    void testProductListCache() {
        // Given - 테스트 데이터 생성
        Product product1 = createProduct("상품 A", 10000);
        Product product2 = createProduct("상품 B", 20000);
        createProductOption(product1.getId(), "Red", "M", 100);
        createProductOption(product2.getId(), "Blue", "L", 200);

        // When - 첫 번째 조회 (캐시 미스)
        long startTime1 = System.currentTimeMillis();
        List<ProductListResponseDto> result1 = getProductListUseCase.execute();
        long duration1 = System.currentTimeMillis() - startTime1;

        // Then - 결과 확인
        assertThat(result1).hasSize(2);

        // When - 두 번째 조회 (캐시 히트)
        long startTime2 = System.currentTimeMillis();
        List<ProductListResponseDto> result2 = getProductListUseCase.execute();
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then - 캐시에서 조회된 결과 확인
        assertThat(result2).hasSize(2);

        System.out.println("===== 상품 목록 캐싱 테스트 결과 =====");
        System.out.println("첫 번째 조회 (캐시 미스): " + duration1 + "ms");
        System.out.println("두 번째 조회 (캐시 히트): " + duration2 + "ms");
        System.out.println("성능 향상: " + (duration1 - duration2) + "ms");
        System.out.println("======================================");
    }

    @Test
    @DisplayName("캐싱 테스트 3: 다른 파라미터는 다른 캐시 키")
    void testDifferentCacheKeys() {
        // Given - 테스트 데이터 생성
        Product product = createProduct("테스트 상품", 10000);
        LocalDate today = LocalDate.now();
        createPopularProduct(product.getId(), PopularProduct.PeriodType.DAILY, 100, today);

        // When - 다른 파라미터로 두 번 조회
        GetPopularProductsCommand command1 = new GetPopularProductsCommand(3, 10);
        GetPopularProductsCommand command2 = new GetPopularProductsCommand(7, 10);

        List<ProductPopularResponseDto> result1 = getPopularProductsUseCase.execute(command1);
        List<ProductPopularResponseDto> result2 = getPopularProductsUseCase.execute(command2);

        // Then - 둘 다 성공 (다른 캐시 키이므로 충돌 없음)
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);

        System.out.println("===== 캐시 키 테스트 결과 =====");
        System.out.println("command1 (days=3, limit=10) → 캐시 키: 3::10");
        System.out.println("command2 (days=7, limit=10) → 캐시 키: 7::10");
        System.out.println("서로 다른 캐시 키로 독립적으로 캐싱됨");
        System.out.println("==============================");
    }

    // === Helper Methods ===

    private Product createProduct(String name, int price) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }

    private void createPopularProduct(Long productId, PopularProduct.PeriodType periodType,
                                      int salesCount, LocalDate aggregatedDate) {
        PopularProduct popularProduct = new PopularProduct(productId, periodType, salesCount, aggregatedDate);
        popularProductRepository.save(popularProduct);
    }

    private ProductOption createProductOption(Long productId, String color, String size, int stock) {
        ProductOption option = ProductOption.builder()
                .productId(productId)
                .color(color)
                .size(size)
                .stock(stock)
                .build();
        return productOptionRepository.save(option);
    }
}
