package com.hhplus.ecommerce.application.usecase.order;

import com.hhplus.ecommerce.application.command.order.CreateOrderCommand;
import com.hhplus.ecommerce.application.usecase.coupon.UseCouponUseCase;
import com.hhplus.ecommerce.application.usecase.stock.DecreaseStockUseCase;
import com.hhplus.ecommerce.application.command.stock.DecreaseStockCommand;
import com.hhplus.ecommerce.application.command.coupon.UseCouponCommand;
import com.hhplus.ecommerce.application.service.product.RankingService;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import com.hhplus.ecommerce.domain.coupon.UserCoupon;
import com.hhplus.ecommerce.domain.order.Order;
import com.hhplus.ecommerce.domain.order.OrderItem;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.ProductOption;
import com.hhplus.ecommerce.domain.stock.StockChangeReason;
import com.hhplus.ecommerce.infrastructure.persistence.base.CouponRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderItemRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.OrderRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.persistence.base.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 주문 생성 UseCase (Redis 분산락 적용)
 *
 * 분산락 적용 이유:
 * - 동일 사용자가 동시에 여러 주문을 생성하는 것을 방지
 * - 재고 차감, 쿠폰 사용 등이 원자적으로 수행되도록 보장
 * - 멀티 인스턴스 환경에서도 동시성 제어
 *
 * 락 키 전략:
 * - "order:user:{userId}" : 사용자 단위 락
 * - 너무 넓은 범위: 전체 주문 락 (X) - 다른 사용자 주문도 막힘
 * - 너무 좁은 범위: 상품 단위 락 (X) - 한 주문에 여러 상품이 있을 수 있음
 * - 적절한 범위: 사용자 단위 락 (O) - 같은 사용자의 동시 주문만 방지
 *
 * 락 타임 설정:
 * - waitTime: 5초 - 다른 주문이 완료될 때까지 대기
 * - leaseTime: 10초 - 주문 생성은 재고 차감, 쿠폰 사용 등이 포함되어 시간이 걸림
 */
@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final DecreaseStockUseCase decreaseStockUseCase;
    private final UseCouponUseCase useCouponUseCase;
    private final RedissonClient redissonClient;
    private final RankingService rankingService;

    /**
     * 주문 생성 (분산락 적용)
     *
     * 락 키: "order:user:{userId}"
     * - 동일 사용자의 동시 주문 방지
     * - 사용자별로 독립적인 락
     *
     * try-finally 패턴:
     * - try: 락 획득 → 비즈니스 로직 실행
     * - finally: 락 해제 (반드시 실행, 데드락 방지)
     * - isHeldByCurrentThread(): 현재 스레드가 락을 보유 중인지 확인
     *
     * InterruptedException 처리:
     * - 스레드가 대기 중 인터럽트되면 발생
     * - Thread.currentThread().interrupt(): 인터럽트 상태 복원
     * - RuntimeException으로 래핑하여 전파
     */
    public Order execute(CreateOrderCommand command) {
        command.validate();

        // 1. 분산락 키 생성: "order:user:123"
        String lockKey = "order:user:" + command.getUserId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도 (waitTime: 5초, leaseTime: 10초)
            // - waitTime: 다른 스레드가 락을 보유 중이면 5초 동안 대기
            // - leaseTime: 락을 획득한 후 10초 뒤 자동 해제 (데드락 방지)
            // - 반환값: true (락 획득 성공), false (락 획득 실패)
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new IllegalStateException("주문 생성 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 3. 비즈니스 로직 실행 (트랜잭션 분리)
            return executeInternal(command);

        } catch (InterruptedException e) {
            // 4. 인터럽트 예외 처리
            Thread.currentThread().interrupt();
            throw new RuntimeException("주문 생성 중 오류가 발생했습니다.", e);
        } finally {
            // 5. 락 해제 (반드시 실행)
            // - isHeldByCurrentThread(): 현재 스레드가 락을 보유 중인지 확인
            // - 다른 스레드의 락을 해제하려 하면 IllegalMonitorStateException 발생
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 주문 생성 비즈니스 로직 (트랜잭션 적용)
     *
     * @Transactional 위치:
     * - execute()가 아닌 executeInternal()에 적용
     * - 이유: 락은 트랜잭션보다 먼저 획득하고 나중에 해제해야 함
     * - 순서: 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
     */
    @Transactional
    protected Order executeInternal(CreateOrderCommand command) {
        // 재고 차감
        for (OrderItem orderItem : command.getOrderItems()) {
            DecreaseStockCommand stockCommand = new DecreaseStockCommand(
                    orderItem.getProductOptionId(),
                    orderItem.getQuantity(),
                    StockChangeReason.ORDER
            );
            decreaseStockUseCase.execute(stockCommand);
        }

        // 쿠폰 사용
        int discountAmount = 0;
        if (command.getUserCouponId() != null) {
            UseCouponCommand couponCommand = new UseCouponCommand(command.getUserCouponId());
            discountAmount = useCouponUseCase.execute(couponCommand);
        }

        // 주문 총액 계산
        int subtotal = command.getOrderItems().stream().mapToInt(i -> i.getQuantity() * i.getPrice()).sum();
        int finalAmount = Math.max(subtotal - discountAmount, 0);

        // 주문 생성 (Builder 패턴)
        Order order = Order.builder()
                .userId(command.getUserId())
                .status(OrderStatus.CREATED)
                .totalAmount(finalAmount)
                .discountAmount(discountAmount)
                .userCouponId(command.getUserCouponId())
                .build();

        Order savedOrder = orderRepository.save(order);

        // OrderItem Batch Insert (N번 INSERT → 1번 Batch INSERT)
        // Builder로 orderId 설정된 새로운 OrderItem 생성
        List<OrderItem> orderItemsWithOrderId = command.getOrderItems().stream()
                .map(item -> OrderItem.builder()
                        .orderId(savedOrder.getId())
                        .productOptionId(item.getProductOptionId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        orderItemRepository.saveAll(orderItemsWithOrderId);

        // 상품 랭킹 업데이트 (STEP 13: Redis Sorted Set)
        // - 주문 완료 시 각 상품의 판매 수량을 Redis에 기록
        // - 실시간 인기 상품 랭킹에 활용
        updateProductRanking(command.getOrderItems());

        return savedOrder;
    }

    /**
     * 상품 랭킹 업데이트
     * - 각 OrderItem의 productOptionId → productId 조회
     * - Redis Sorted Set에 판매 수량 누적
     */
    private void updateProductRanking(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                // ProductOption 조회하여 productId 가져오기
                ProductOption option = productOptionRepository.findById(item.getProductOptionId())
                        .orElse(null);

                if (option != null) {
                    // Redis 랭킹 업데이트 (productId, 판매 수량)
                    rankingService.incrementProductScore(option.getProductId(), item.getQuantity());
                }
            } catch (Exception e) {
                // 랭킹 업데이트 실패 시 로깅만 하고 주문은 계속 진행
                // (랭킹은 부가 기능이므로 주문 실패로 이어지면 안 됨)
            }
        }
    }
}
