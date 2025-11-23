package com.hhplus.ecommerce.application.usecase.point;

import com.hhplus.ecommerce.application.command.point.UsePointCommand;
import com.hhplus.ecommerce.common.util.RetryUtils;
import com.hhplus.ecommerce.domain.point.Point;
import com.hhplus.ecommerce.domain.point.PointHistory;
import com.hhplus.ecommerce.domain.point.PointType;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 사용 UseCase
 * - 낙관적 락 (@Version) 사용
 * - OptimisticLockException 발생 시 최대 30회 재시도 (지수 백오프)
 *
 * 재시도 전략:
 * - 최대 재시도: 30회
 * - 백오프: 지수 백오프 (1ms, 2ms, 4ms, ..., 최대 100ms)
 * - 누적 최대 대기: 약 900ms
 *
 * 재시도 횟수 근거:
 * - 포인트 사용은 결제 시 발생하여 충돌 빈도가 중간 수준
 * - 재고/쿠폰 대비 상대적으로 낮은 충돌률
 */
@Component
@RequiredArgsConstructor
public class UsePointUseCase {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private static final int MAX_RETRIES = 30;
    private static final long MAX_BACKOFF_MS = 100L;

    public void execute(UsePointCommand command) {
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
                    throw new IllegalStateException("포인트 사용 실패: 인터럽트", ie);
                }
            } catch (IllegalArgumentException e) {
                // 포인트 부족 등 비즈니스 규칙 위반 시 즉시 실패
                throw e;
            }
        }

        throw new IllegalStateException("포인트 사용 실패: 재시도 한도 초과");
    }

    @Transactional
    protected void executeInternal(UsePointCommand command) {
        Point point = pointRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("포인트 정보를 찾을 수 없습니다."));

        // Entity 메서드 사용 - 비즈니스 규칙은 Entity에서 검증
        point.use(command.getAmount());

        // 낙관적 락 검증을 위해 flush
        pointRepository.saveAndFlush(point);

        // PointHistory 기록
        PointHistory history = new PointHistory(
                command.getUserId(),
                -command.getAmount(),  // 음수로 저장 (사용)
                PointType.USE,
                command.getReason()
        );
        pointHistoryRepository.save(history);
    }
}
