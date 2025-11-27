package com.hhplus.ecommerce.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐싱 설정 (Master-Replica 구조)
 *
 * Spring Cache란?
 * - Spring의 캐시 추상화 레이어
 * - @Cacheable, @CacheEvict, @CachePut 애노테이션 제공
 * - 코드 변경 없이 캐시 구현체 교체 가능 (Redis, Ehcache 등)
 *
 * 왜 Redis 캐싱을 사용하나?
 * 1. 읽기 성능 향상: DB 조회 대신 메모리에서 조회
 * 2. DB 부하 감소: 동일 쿼리 반복 실행 방지
 * 3. 분산 환경 지원: 여러 서버가 동일 캐시 공유
 * 4. TTL 관리: 자동 만료로 데이터 신선도 유지
 *
 * Master-Replica 캐싱 전략:
 * - 캐시 쓰기 (CachePut): Master에 저장 → Replica로 복제
 * - 캐시 읽기 (Cacheable): Replica에서 조회 (부하 분산)
 * - 캐시 삭제 (CacheEvict): Master에서 삭제 → Replica로 전파
 *
 * 캐시 키 전략:
 * - 인기 상품: popularProducts::{days}::{limit}
 * - 상품 목록: productList::SimpleKey[]
 * - TTL: 데이터 변동성에 따라 30초 ~ 5분
 */
@Configuration
@EnableCaching  // Spring Cache 활성화
public class RedisCacheConfig {

    /**
     * CacheManager Bean 생성
     *
     * RedisConnectionFactory:
     * - application.yml의 Redis 설정 사용
     * - Lettuce Client가 Master-Replica 연결 관리
     * - 읽기: Replica, 쓰기: Master (자동 라우팅)
     *
     * RedisCacheConfiguration:
     * - 기본 TTL, 직렬화 방식 설정
     * - 캐시별 커스텀 TTL 설정 가능
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 기본 TTL: 1분 (데이터 변동성이 낮은 경우)
                .entryTtl(Duration.ofMinutes(1))
                // 키 직렬화: String (가독성)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 값 직렬화: JSON (Object 저장 가능, 디버깅 용이)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                // null 값 캐싱 비활성화 (null은 캐시하지 않음)
                .disableCachingNullValues();

        // 캐시별 커스텀 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 1. 인기 상품 캐시: TTL 5분
        //    - 이유: 집계 데이터로 변동이 적음, 조회 빈도 높음
        //    - 효과: DB 부하 최소화, 응답 속도 향상
        cacheConfigurations.put("popularProducts",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 2. 상품 목록 캐시: TTL 30초
        //    - 이유: 재고 변동이 자주 발생, 신선도 중요
        //    - 효과: 짧은 캐시로 데이터 일관성 유지
        cacheConfigurations.put("productList",
                defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // CacheManager 생성 및 반환
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * ObjectMapper 설정
     *
     * GenericJackson2JsonRedisSerializer 사용:
     * - Object를 JSON으로 직렬화/역직렬화
     * - 타입 정보 포함 (@class 필드 추가)
     * - 역직렬화 시 원본 타입으로 복원
     *
     * JavaTimeModule:
     * - LocalDate, LocalDateTime 등 Java 8 Time API 지원
     * - ISO-8601 포맷으로 직렬화 (예: "2024-01-01")
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 Time API 지원 (LocalDate, LocalDateTime 등)
        mapper.registerModule(new JavaTimeModule());

        // 날짜를 타임스탬프 대신 ISO-8601 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 타입 정보 포함 (역직렬화 시 타입 복원)
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }
}
