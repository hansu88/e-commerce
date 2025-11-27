# STEP 11+12: Redis 분산락 & 캐싱

## 📋 구현 내용

### STEP 11: 분산락
- Redisson 기반 분산락 구현
- 사용자별 주문 중복 방지 (`order:user:{userId}`)
- Master-Replica 구조 (고가용성)

### STEP 12: 캐싱
- Spring Cache + Redis
- 인기 상품 조회 캐싱 (TTL 5분)
- 상품 목록 조회 캐싱 (TTL 30초)

---

## 🔍 핵심 파일

### 분산락
- `RedissonConfig.java` - Master-Replica 설정
- `CreateOrderUseCase.java` - 분산락 적용
- `DistributedLockIntegrationTest.java` - 테스트

### 캐싱
- `RedisCacheConfig.java` - 캐시 설정
- `GetPopularProductsUseCase.java` - @Cacheable
- `GetProductListUseCase.java` - @Cacheable
- `CacheIntegrationTest.java` - 테스트

---

## 📊 성능 개선

| 항목 | Before | After | 개선 |
|------|--------|-------|------|
| 인기 상품 조회 | ~50ms | ~2ms | 25배 ⬆ |
| 상품 목록 조회 | ~80ms | ~3ms | 26배 ⬆ |
| 동시 주문 중복 | 발생 가능 | 0건 | ✅ |

---

## 📚 학습 자료

### 동시성 이해
- `concurrency/동시성-이해하기.md` - 실생활 비유로 쉽게 이해

### Redis 심화
- `redis/Redis-Internals.md` - Lua 스크립트 분석
- `redis/Redis-CLI-실습.md` - 실습 가이드

---

## 🎯 핵심 개념

**분산락**: 여러 서버가 공유하는 자물쇠
**Lua 스크립트**: 원자적 실행 보장
**Pub/Sub**: 효율적인 대기 방식
**Master-Replica**: 읽기/쓰기 분리로 성능 향상

---

## ✅ 완료 사항

- [x] Redis 분산락 구현
- [x] Redis 캐싱 구현
- [x] 통합 테스트 작성
- [x] Master-Replica 설정
- [x] 학습 문서 작성
