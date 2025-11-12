package com.hhplus.ecommerce.application.service;

import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * 재고 로직 핵심 테스트
 * - 재고 차감 성공
 * - 재고 부족 예외
 * - 재고 증가 성공
 */
@SpringBootTest
class StockServiceTest {

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private StockService stockService;



    @Test
    @Transactional
    @DisplayName("재고 차감 성공 - 주문 시 재고가 정상적으로 차감되어야 한다")
    void decreaseStockSuccess() {
        // Given - 재고 10개인 상품 옵션
        ProductOption option = new ProductOption(1L, "Black", "260", 10);
        ProductOption savedOption = productOptionRepository.save(option);

        // When - 5개 차감
        stockService.decreaseStock(savedOption.getId(), 5, StockChangeReason.ORDER);

        // Then - 재고가 5개로 감소
        ProductOption result = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(5);
    }

    @Test
    @Transactional
    @DisplayName("재고 부족 예외 - 요청 수량보다 재고가 적으면 예외가 발생해야 한다")
    void decreaseStockOutOfStockThrowsException() {
        // Given - 재고 3개인 상품 옵션
        ProductOption option = new ProductOption(1L, "White", "270", 3);
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
    @Transactional
    @DisplayName("재고 증가 성공 - 주문 취소 시 재고가 정상적으로 증가해야 한다")
    void increaseStockSuccess() {
        // Given - 재고 5개인 상품 옵션
        ProductOption option = new ProductOption(1L, "Blue", "260", 5);
        ProductOption savedOption = productOptionRepository.save(option);

        // When - 3개 증가 (취소)
        stockService.increaseStock(savedOption.getId(), 3, StockChangeReason.CANCEL);

        // Then - 재고가 8개로 증가
        ProductOption result = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(8);
    }
}
