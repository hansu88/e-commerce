package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.DecreaseStockCommand;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 재고 차감 UseCase
 * - 동시성 제어 적용 (ProductOption ID별 Lock)
 */
@Component
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final StockHistoryRepository stockHistoryRepository;

    // ProductOption ID별 Lock 객체 관리
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public void execute(DecreaseStockCommand command) {
        // ProductOption ID별 Lock 객체 획득
        Object lock = lockMap.computeIfAbsent(command.getProductOptionId(), k -> new Object());

        synchronized (lock) {
            ProductOption option = productOptionRepository.findById(command.getProductOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다: " + command.getProductOptionId()));

            // 재고 부족 검증
            if (option.getStock() < command.getQuantity()) {
                throw new OutOfStockException(
                        String.format("재고 부족: %s %s (요청: %d, 재고: %d)",
                                option.getColor(), option.getSize(), command.getQuantity(), option.getStock())
                );
            }

            // 재고 차감
            option.setStock(option.getStock() - command.getQuantity());
            productOptionRepository.save(option);

            // StockHistory 기록 (음수로 저장)
            StockHistory history = new StockHistory(command.getProductOptionId(), -command.getQuantity(), command.getReason());
            stockHistoryRepository.save(history);
        }
    }
}
