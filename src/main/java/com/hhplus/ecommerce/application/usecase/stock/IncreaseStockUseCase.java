package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.stock.IncreaseStockCommand;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 증가 UseCase
 * - 낙관적 락 (@Version) 사용
 */
@Component
@RequiredArgsConstructor
public class IncreaseStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final StockHistoryRepository stockHistoryRepository;

    /**
     * 재고 증가
     *
     * @CacheEvict: 재고가 변경되므로 productList 캐시 무효화
     * - 주문 취소 시 재고 복구할 때 사용
     */
    @CacheEvict(value = "productList", allEntries = true)
    @Transactional
    public void execute(IncreaseStockCommand command) {
        ProductOption option = productOptionRepository.findById(command.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + command.getProductOptionId()));

        // 비즈니스 메서드 사용
        option.increaseStock(command.getQuantity());
        productOptionRepository.save(option);

        StockHistory history = new StockHistory(command.getProductOptionId(), command.getQuantity(), command.getReason());
        stockHistoryRepository.save(history);
    }
}
