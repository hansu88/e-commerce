package com.hhplus.ecommerce.common.util;

/**
 * 재시도 로직 공통 유틸리티
 *
 * 낙관적 락 충돌 발생 시 재시도 전략을 제공합니다.
 * 지수 백오프(Exponential Backoff) 방식을 사용하여
 * 초기에는 빠르게 재시도하고, 충돌이 계속되면 대기 시간을 늘립니다.
 */
public class RetryUtils {

    /**
     * 지수 백오프 대기 시간 계산
     *
     * @param retryCount 현재 재시도 횟수 (0부터 시작)
     * @param maxDelayMs 최대 대기 시간 (밀리초)
     * @return 대기할 시간 (밀리초)
     *
     * 예시:
     * - retry 0: 1ms
     * - retry 1: 2ms
     * - retry 2: 4ms
     * - retry 3: 8ms
     * - retry 4: 16ms
     * - retry 5: 32ms
     * - retry 6: 64ms
     * - retry 7~: 100ms (maxDelayMs)
     */
    public static long calculateExponentialBackoff(int retryCount, long maxDelayMs) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다.");
        }
        if (maxDelayMs <= 0) {
            throw new IllegalArgumentException("최대 대기 시간은 양수여야 합니다.");
        }

        // 2^retryCount 계산, 최대값 제한
        long delay = (long) Math.pow(2, retryCount);
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 재시도 대기
     *
     * @param retryCount 현재 재시도 횟수
     * @param maxDelayMs 최대 대기 시간
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static void backoff(int retryCount, long maxDelayMs) throws InterruptedException {
        long delay = calculateExponentialBackoff(retryCount, maxDelayMs);
        Thread.sleep(delay);
    }
}
