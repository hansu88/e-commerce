package com.hhplus.ecommerce.application.usecase.point;

import com.hhplus.ecommerce.application.command.point.EarnPointCommand;
import com.hhplus.ecommerce.domain.point.Point;
import com.hhplus.ecommerce.domain.point.PointHistory;
import com.hhplus.ecommerce.domain.point.PointType;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 적립 UseCase
 */
@Component
@RequiredArgsConstructor
public class EarnPointUseCase {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void execute(EarnPointCommand command) {
        // Point 조회 또는 생성
        Point point = pointRepository.findByUserId(command.getUserId())
                .orElseGet(() -> pointRepository.save(new Point(command.getUserId(), 0)));

        // 포인트 적립
        point.earn(command.getAmount());
        pointRepository.save(point);

        // 이력 저장
        PointHistory history = new PointHistory(
                command.getUserId(),
                command.getAmount(),
                PointType.EARN,
                command.getReason()
        );
        pointHistoryRepository.save(history);
    }
}
