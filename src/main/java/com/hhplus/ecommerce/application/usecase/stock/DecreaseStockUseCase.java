package com.hhplus.ecommerce.application.usecase.stock;

import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.common.util.RetryUtils;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockHistory;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.StockHistoryRepository;
import com.hhplus.ecommerce.presentation.exception.OutOfStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 차감 UseCase
 * - 낙관적 락 (@Version) 사용
 * - OptimisticLockException 발생 시 최대 30회 재시도 (지수 백오프)
 *
 * 재시도 전략:
 * - 최대 재시도: 30회
 * - 백오프: 지수 백오프 (1ms, 2ms, 4ms, ..., 최대 100ms)
 * - 누적 최대 대기: 약 900ms
 *
 * 재시도 횟수 근거:
 * - 재고 차감은 주문 시 발생하여 충돌 빈도가 중간 수준
 * - 적절한 재시도로 성공률과 응답 시간 균형
 */
@Component
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final StockHistoryRepository stockHistoryRepository;

    private static final int MAX_RETRIES = 30;
    private static final long MAX_BACKOFF_MS = 100L;

    /**
     * 재고 차감
     *
     * @CacheEvict: 재고가 변경되므로 productList 캐시 무효화
     * - allEntries = true: 캐시 전체 삭제 (어떤 상품인지 모르므로)
     * - 다음 조회 시 DB에서 최신 재고 정보 조회
     */
    @CacheEvict(value = "productList", allEntries = true)
    public void execute(DecreaseStockCommand command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                executeInternal(command);
                return;
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                try {
                    RetryUtils.backoff(retryCount, MAX_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재고 차감 실패: 인터럽트", ie);
                }
            } catch (OutOfStockException | IllegalArgumentException e) {
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
