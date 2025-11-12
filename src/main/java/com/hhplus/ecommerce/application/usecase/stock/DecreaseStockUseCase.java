package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 차감 UseCase
 * - 낙관적 락 (@Version) 사용
 * - OptimisticLockException 발생 시 최대 5회 재시도
 */
@Component
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private static final int MAX_RETRIES = 20;  // 추후 retry 가 필요하다고 판단시 작업

    public void execute(DecreaseStockCommand command) {
        executeInternal(command);
    }

    @Transactional
    protected void executeInternal(DecreaseStockCommand command) {
        ProductOption option = productOptionRepository.findById(command.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + command.getProductOptionId()));

        // 재고 부족 검증
        if (option.getStock() < command.getQuantity()) {
            throw new OutOfStockException(
                    String.format("재고 부족: %s %s (요청: %d, 재고: %d)",
                            option.getColor(), option.getSize(), command.getQuantity(), option.getStock())
            );
        }

        // 재고 차감 (낙관적 락으로 동시성 제어)
        option.setStock(option.getStock() - command.getQuantity());
        productOptionRepository.save(option);

        // StockHistory 기록 (음수로 저장)
        StockHistory history = new StockHistory(command.getProductOptionId(), -command.getQuantity(), command.getReason());
        stockHistoryRepository.save(history);
    }
}
