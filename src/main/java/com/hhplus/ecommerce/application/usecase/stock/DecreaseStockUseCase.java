package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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

    private static final int MAX_RETRIES = 30;

    public void execute(DecreaseStockCommand command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                executeInternal(command);
                return;
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                try {
                    Thread.sleep(retryCount * 5L); // 점진적 backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재고 차감 실패: 인터럽트", ie);
                }
            } catch (IllegalArgumentException e) {
                // 재고 부족, 잘못된 수량 등 비즈니스 규칙 위반 시 즉시 실패
                throw e; 
            }
        }

        throw new IllegalStateException("재고 차감 실패: 재시도 한도 초과");
    }

    @Transactional
    protected void executeInternal(DecreaseStockCommand command) {
        ProductOption option = productOptionRepository.findById(command.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

        // Entity 메서드 사용 비즈니스 규칙은 Entity에서 검증
        option.decreaseStock(command.getQuantity());

        // 낙관적 락 검증을 위해 flush
        productOptionRepository.saveAndFlush(option);

        // StockHistory 기록
        StockHistory history = new StockHistory(command.getProductOptionId(),
                -command.getQuantity(), command.getReason());
        stockHistoryRepository.save(history);
    }
}
