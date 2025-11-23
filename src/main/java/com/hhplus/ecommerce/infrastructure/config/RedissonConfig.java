package com.hhplus.ecommerce.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 분산락 설정
 *
 * Redisson이란?
 * - Redis 기반의 Java 분산 객체 프레임워크
 * - 분산락, Pub/Sub, 분산 컬렉션 등 제공
 * - Lettuce보다 고수준 추상화
 *
 * 왜 Redisson을 사용하나?
 * - 분산락 기능이 내장되어 있음
 * - tryLock, unlock 등의 API 제공
 * - Pub/Sub 기반 대기 알림 (polling 불필요)
 * - 데드락 방지 (leaseTime, watchDog)
 *
 * Single Server vs Cluster:
 * - Single Server: 단일 Redis 서버 (우리 환경)
 * - Cluster: Redis 클러스터 (고가용성)
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * RedissonClient Bean 생성
     *
     * useSingleServer() 설정:
     * - address: redis://host:port 형식
     * - password: Redis 인증 비밀번호
     * - connectionPoolSize: 연결 풀 크기 (기본 64)
     * - connectionMinimumIdleSize: 최소 유휴 연결 수 (기본 24)
     * - timeout: 응답 타임아웃 3초
     * - retryAttempts: 재시도 횟수 3회
     * - retryInterval: 재시도 간격 1.5초
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword)           // Redis 인증 비밀번호
                .setConnectionPoolSize(50)            // 연결 풀 크기
                .setConnectionMinimumIdleSize(10)     // 최소 유휴 연결
                .setTimeout(3000)                      // 응답 타임아웃 3초
                .setRetryAttempts(3)                   // 재시도 3회
                .setRetryInterval(1500);               // 재시도 간격 1.5초

        return Redisson.create(config);
    }
}
