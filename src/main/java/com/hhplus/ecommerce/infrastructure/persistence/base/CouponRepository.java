package com.hhplus.ecommerce.infrastructure.persistence.base;

import com.hhplus.ecommerce.domain.coupon.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 전체 쿠폰(Coupon) 관리 Repository 인터페이스
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    /**
     * 비관적 락(PESSIMISTIC_WRITE)으로 쿠폰 조회
     *
     * 선착순 쿠폰 발급 시나리오에 사용:
     * - SELECT ... FOR UPDATE로 행 잠금
     * - 다른 트랜잭션은 락이 해제될 때까지 대기
     * - 충돌이 매우 빈번한 시나리오에 적합
     *
     * @param id 쿠폰 ID
     * @return 쿠폰 (비관적 락 적용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithPessimisticLock(@Param("id") Long id);
}
