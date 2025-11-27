package com.hhplus.ecommerce.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 분산락 & 캐싱 설정 (Master-Replica 구조)
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
 * Master-Replica 구조:
 * - Master (Primary): 쓰기 작업 (분산락, 캐시 쓰기)
 * - Replica (Slave): 읽기 작업 (캐시 읽기) → 부하 분산
 * - 고가용성: Primary 장애 시 Replica에서 읽기 가능
 *
 * 왜 Master-Replica를 사용하나?
 * 1. 고가용성: Primary 다운 시에도 서비스 유지
 * 2. 부하 분산: 읽기는 Replica에서 처리 → Primary 부담 감소
 * 3. 실무 환경: 운영 환경과 동일한 구조
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisMasterPort;

    @Value("${spring.data.redis.replica.port:6380}")
    private int redisReplicaPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * RedissonClient Bean 생성 (Master-Replica 모드)
     *
     * useMasterSlaveServers() 설정:
     * - setMasterAddress: Master(Primary) 서버 주소 (쓰기)
     * - addSlaveAddress: Replica(Slave) 서버 주소 (읽기)
     * - setReadMode: 읽기 전략
     *   - SLAVE: Replica에서만 읽기 (부하 분산 최대화)
     *   - MASTER_SLAVE: Master와 Replica에서 읽기 (균형)
     *   - MASTER: Master에서만 읽기 (안전성 최대)
     * - setSubscriptionMode: Pub/Sub 구독 모드
     *   - MASTER: Master에서만 구독 (일관성 보장)
     *   - SLAVE: Replica에서 구독
     *
     * 동작 방식:
     * 1. 쓰기 (분산락, 캐시 쓰기): Master로 전송
     * 2. 읽기 (캐시 읽기): Replica에서 처리 (SLAVE 모드)
     * 3. Replica 장애 시: 자동으로 Master로 Fallback
     * 4. 동기화: Master → Replica 자동 복제
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useMasterSlaveServers()
                // Master(Primary) 서버: 쓰기 작업
                .setMasterAddress("redis://" + redisHost + ":" + redisMasterPort)
                // Replica(Slave) 서버: 읽기 작업 (부하 분산)
                .addSlaveAddress("redis://" + redisHost + ":" + redisReplicaPort)
                // Redis 인증 비밀번호
                .setPassword(redisPassword)
                // 읽기 모드: SLAVE (Replica에서만 읽기, 부하 분산 최대화)
                // - 캐시 조회는 Replica에서 → Primary 부담 감소
                // - Replica 장애 시 자동으로 Master로 Fallback
                .setReadMode(ReadMode.SLAVE)
                // Pub/Sub 구독 모드: MASTER (일관성 보장)
                // - 분산락의 Pub/Sub는 Master에서만 처리
                .setSubscriptionMode(SubscriptionMode.MASTER)
                // 연결 풀 설정
                .setMasterConnectionPoolSize(50)      // Master 연결 풀
                .setSlaveConnectionPoolSize(50)       // Replica 연결 풀
                .setMasterConnectionMinimumIdleSize(10)
                .setSlaveConnectionMinimumIdleSize(10)
                // 타임아웃 및 재시도 설정
                .setTimeout(3000)                      // 응답 타임아웃 3초
                .setRetryAttempts(3)                   // 재시도 3회
                .setRetryInterval(1500);               // 재시도 간격 1.5초

        return Redisson.create(config);
    }
}
