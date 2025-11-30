package com.hhplus.ecommerce.application.usecase.point;

import com.hhplus.ecommerce.application.command.point.UsePointCommand;
import com.hhplus.ecommerce.common.util.RetryUtils;
import com.hhplus.ecommerce.domain.point.Point;
import com.hhplus.ecommerce.domain.point.PointHistory;
import com.hhplus.ecommerce.domain.point.PointType;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 포인트 사용 UseCase
 * - 분산락 + 낙관적 락 이중 보호
 * - 분산락: 동일 사용자의 동시 포인트 사용 방지
 * - 낙관적 락: DB 레벨 충돌 감지 (백업)
 *
 * 왜 분산락을 추가했나?
 * - 낙관적 락만으로는 재시도가 많아 성능 저하
 * - 사용자별 분산락으로 순차 처리 → 재시도 불필요
 * - 주문과 동일한 패턴 (사용자별 제어)
 */
@Component
@RequiredArgsConstructor
public class UsePointUseCase {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;

    /**
     * 포인트 사용 (분산락 적용)
     *
     * 락 키: point:user:{userId}
     * - 동일 사용자의 포인트 사용을 순차 처리
     * - 다른 사용자는 병렬 처리 가능
     *
     * 설정:
     * - waitTime: 5초 (락 획득 대기)
     * - leaseTime: 10초 (자동 해제)
     */
    public void execute(UsePointCommand command) {
        // 1. 분산락 키 생성 (사용자별)
        String lockKey = "point:user:" + command.getUserId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new IllegalStateException("포인트 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 3. 비즈니스 로직 실행 (트랜잭션 분리)
            executeInternal(command);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 사용 중 오류가 발생했습니다.", e);
        } finally {
            // 4. 락 해제 (반드시 실행)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 포인트 사용 내부 로직
     *
     * 분산락으로 이미 순차 처리 보장
     * → 낙관적 락 재시도 불필요
     * → 단순하고 빠른 처리
     */
    @Transactional
    protected void executeInternal(UsePointCommand command) {
        Point point = pointRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("포인트 정보를 찾을 수 없습니다."));

        // Entity 메서드 사용 - 비즈니스 규칙은 Entity에서 검증
        point.use(command.getAmount());

        // 저장 (낙관적 락은 백업으로 유지)
        pointRepository.save(point);

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
