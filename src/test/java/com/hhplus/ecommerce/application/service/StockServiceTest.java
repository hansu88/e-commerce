package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.product.ProductOptionRepository;
import com.hhplus.ecommerce.domain.stock.OutOfStockException;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.memory.InMemoryProductOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재고 로직 핵심 테스트
 * - 재고 차감 성공
 * - 재고 부족 예외
 * - 재고 증가 성공
 */
class StockServiceTest {

    private ProductOptionRepository productOptionRepository;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        // InMemory Repository 사용 (실제 구현체)
        productOptionRepository = new InMemoryProductOptionRepository();
        stockService = new StockService(productOptionRepository);
    }

    @Test
    @DisplayName("재고 차감 성공 - 주문 시 재고가 정상적으로 차감되어야 한다")
    void decreaseStock_Success() {
        // Given - 재고 10개인 상품 옵션
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Black");
        option.setSize("260");
        option.setStock(10);
        ProductOption savedOption = productOptionRepository.save(option);

        // When - 5개 차감
        stockService.decreaseStock(savedOption.getId(), 5, StockChangeReason.ORDER);

        // Then - 재고가 5개로 감소
        ProductOption result = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고 부족 예외 - 요청 수량보다 재고가 적으면 예외가 발생해야 한다")
    void decreaseStock_OutOfStock_ThrowsException() {
        // Given - 재고 3개인 상품 옵션
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("White");
        option.setSize("270");
        option.setStock(3);
        ProductOption savedOption = productOptionRepository.save(option);

        // When & Then - 5개 요청 시 예외 발생
        assertThatThrownBy(() ->
                stockService.decreaseStock(savedOption.getId(), 5, StockChangeReason.ORDER))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("재고 부족");

        // 재고는 변경되지 않음
        ProductOption result = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("재고 증가 성공 - 주문 취소 시 재고가 정상적으로 증가해야 한다")
    void increaseStock_Success() {
        // Given - 재고 5개인 상품 옵션
        ProductOption option = new ProductOption();
        option.setProductId(1L);
        option.setColor("Blue");
        option.setSize("260");
        option.setStock(5);
        ProductOption savedOption = productOptionRepository.save(option);

        // When - 3개 증가 (취소)
        stockService.increaseStock(savedOption.getId(), 3, StockChangeReason.CANCEL);

        // Then - 재고가 8개로 증가
        ProductOption result = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(8);
    }
}
