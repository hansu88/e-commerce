package com.hhplus.ecommerce.application.service.coupon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 서비스 (Redis 기반 선착순 처리)
 * - INCR: 원자적 발급 수량 증가
 * - SADD: 사용자별 중복 발급 방지
 *
 * 흐름:
 * 1. Redis에서 빠르게 선착순 체크
 * 2. 통과 시 DB 저장
 * 3. 실패 시 즉시 리턴 (DB 접근 안 함)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis 키 상수
     */
    private static final String ISSUED_KEY_PREFIX = "coupon:issued:";   // 발급 수량
    private static final String USERS_KEY_PREFIX = "coupon:users:";     // 발급 받은 사용자

    /**
     * 선착순 쿠폰 발급 체크
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @param totalLimit 총 한도
     * @return true: 발급 가능, false: 한도 초과
     * @throws IllegalStateException 중복 발급 시
     */
    public boolean tryIssueCoupon(Long couponId, Long userId, int totalLimit) {
        String issuedKey = ISSUED_KEY_PREFIX + couponId;
        String usersKey = USERS_KEY_PREFIX + couponId;
        String userValue = "user:" + userId;

        try {
            // 1. 중복 발급 체크 (SISMEMBER)
            Boolean alreadyIssued = redisTemplate.opsForSet()
                    .isMember(usersKey, userValue);

            if (Boolean.TRUE.equals(alreadyIssued)) {
                log.warn("중복 발급 시도 - 쿠폰 ID: {}, 사용자 ID: {}", couponId, userId);
                throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
            }

            // 2. 발급 수량 증가 (INCR - 원자적!)
            Long issuedCount = redisTemplate.opsForValue()
                    .increment(issuedKey, 1);

            // 3. 한도 체크
            if (issuedCount == null || issuedCount > totalLimit) {
                // 한도 초과 시 롤백 (감소)
                redisTemplate.opsForValue().decrement(issuedKey, 1);
                log.info("쿠폰 한도 초과 - 쿠폰 ID: {}, 발급 수량: {}/{}",
                        couponId, issuedCount, totalLimit);
                return false;
            }

            // 4. 사용자 Set에 추가 (SADD - 중복 방지)
            redisTemplate.opsForSet().add(usersKey, userValue);

            log.info("선착순 쿠폰 통과 - 쿠폰 ID: {}, 사용자 ID: {}, 발급 수량: {}/{}",
                    couponId, userId, issuedCount, totalLimit);
            return true;

        } catch (IllegalStateException e) {
            // 중복 발급은 그대로 전파
            throw e;
        } catch (Exception e) {
            // Redis 오류 시 로깅 후 실패 처리
            log.error("Redis 선착순 체크 실패 - 쿠폰 ID: {}, 사용자 ID: {}",
                    couponId, userId, e);
            return false;
        }
    }

    /**
     * 현재 발급 수량 조회
     *
     * @param couponId 쿠폰 ID
     * @return 발급 수량 (없으면 0)
     */
    public int getIssuedCount(Long couponId) {
        try {
            String key = ISSUED_KEY_PREFIX + couponId;
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count) : 0;

        } catch (Exception e) {
            log.error("발급 수량 조회 실패 - 쿠폰 ID: {}", couponId, e);
            return 0;
        }
    }

    /**
     * 남은 수량 조회
     *
     * @param couponId 쿠폰 ID
     * @param totalLimit 총 한도
     * @return 남은 수량
     */
    public int getRemainingCount(Long couponId, int totalLimit) {
        int issued = getIssuedCount(couponId);
        return Math.max(totalLimit - issued, 0);
    }

    /**
     * 사용자 발급 여부 확인
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return true: 이미 발급받음, false: 발급 안 받음
     */
    public boolean isAlreadyIssued(Long couponId, Long userId) {
        try {
            String usersKey = USERS_KEY_PREFIX + couponId;
            String userValue = "user:" + userId;

            Boolean isMember = redisTemplate.opsForSet()
                    .isMember(usersKey, userValue);

            return Boolean.TRUE.equals(isMember);

        } catch (Exception e) {
            log.error("발급 여부 확인 실패 - 쿠폰 ID: {}, 사용자 ID: {}",
                    couponId, userId, e);
            return false;
        }
    }

    /**
     * 쿠폰 발급 수량 초기화 (관리자 기능 / 테스트용)
     *
     * @param couponId 쿠폰 ID
     */
    public void resetCouponIssued(Long couponId) {
        try {
            String issuedKey = ISSUED_KEY_PREFIX + couponId;
            String usersKey = USERS_KEY_PREFIX + couponId;

            redisTemplate.delete(issuedKey);
            redisTemplate.delete(usersKey);

            log.info("쿠폰 발급 데이터 초기화 - 쿠폰 ID: {}", couponId);

        } catch (Exception e) {
            log.error("쿠폰 초기화 실패 - 쿠폰 ID: {}", couponId, e);
        }
    }

    /**
     * Redis와 DB 정합성 동기화 (스케줄러용)
     *
     * @param couponId 쿠폰 ID
     * @param dbIssuedCount DB의 실제 발급 수량
     */
    public void syncWithDatabase(Long couponId, int dbIssuedCount) {
        try {
            String issuedKey = ISSUED_KEY_PREFIX + couponId;
            redisTemplate.opsForValue().set(issuedKey, String.valueOf(dbIssuedCount));

            log.info("Redis-DB 정합성 동기화 - 쿠폰 ID: {}, 수량: {}",
                    couponId, dbIssuedCount);

        } catch (Exception e) {
            log.error("정합성 동기화 실패 - 쿠폰 ID: {}", couponId, e);
        }
    }
}
