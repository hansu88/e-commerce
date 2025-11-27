# 이커머스 프로젝트 문서 모음

## 📁 문서 구조

### 1. 동시성 제어 문서 (`concurrency/`)
- **동시성-이해하기.md** - 동시성 개념을 실생활 비유와 그림으로 완벽 이해
- **01-문제-식별.md** - 동시성 문제 발견 및 분석
- **02-STEP8-쿼리분석및최적화.md** - N+1 문제 해결
- **03-STEP9-동시성제어개선.md** - 비관적 락 + 지수 백오프
- **04-STEP10-쿼리최적화.md** - 인덱스 및 집계 테이블


### 2. Redis 심화 문서 (`redis/`)
- **Redis-Internals.md** - Redisson Lua 스크립트 완전 분해
- **Redis-CLI-실습.md** - 실전 Redis CLI 가이드

### 3. 성능 분석 문서 (`report/`)
- **performance-analysis.md** - 성능 테스트 결과

---

## 🎯 학습 순서 (추천)

### Step 1: 동시성 기본 개념 이해
```
📖 동시성-이해하기.md 읽기
- Race Condition이란?
- Critical Section이란?
- 분산락 동작 원리
```

### Step 2: Redis 깊게 파기
```
📖 Redis-Internals.md 읽기
- Redisson Lua 스크립트 분석
- tryLock / unlock 내부 동작
- Pub/Sub 대기 메커니즘

📖 Redis-CLI-실습.md 따라하기
- 실제 Redis에서 Key-Value 확인
- MONITOR로 명령어 흐름 관찰
```

### Step 3: 코드 실습
```
💻 DistributedLockIntegrationTest 실행
💻 CacheIntegrationTest 실행
💻 Redis CLI로 직접 확인
```

---

## 🔍 핵심 파일 위치

### 분산락 구현
- **설정**: `src/main/java/com/hhplus/ecommerce/infrastructure/config/RedissonConfig.java`
- **사용**: `src/main/java/com/hhplus/ecommerce/application/usecase/order/CreateOrderUseCase.java`
- **테스트**: `src/test/java/com/hhplus/ecommerce/distributedlock/DistributedLockIntegrationTest.java`

### 캐싱 구현
- **설정**: `src/main/java/com/hhplus/ecommerce/infrastructure/config/RedisCacheConfig.java`
- **사용**:
  - `src/main/java/com/hhplus/ecommerce/application/usecase/product/GetPopularProductsUseCase.java`
  - `src/main/java/com/hhplus/ecommerce/application/usecase/product/GetProductListUseCase.java`
- **테스트**: `src/test/java/com/hhplus/ecommerce/cache/CacheIntegrationTest.java`

---

## 📊 성능 개선 결과

### Before (최적화 전)
```
- 인기 상품 조회: ~50ms (DB 쿼리)
- 상품 목록 조회: ~80ms (N+1 쿼리)
- 동시 주문: 중복 발생 가능
```

### After (최적화 후)
```
- 인기 상품 조회: ~2ms (캐시 히트 시, 25배 빠름)
- 상품 목록 조회: ~3ms (캐시 히트 시, 26배 빠름)
- 동시 주문: 분산락으로 중복 0건
```

---

## 💡 핵심 개념 요약

### 분산락
```
- 목적: 동시 주문 방지
- 구현: Redisson (Lua 스크립트)
- 키: order:user:{userId}
- TTL: 10초 (데드락 방지)
- 대기: Pub/Sub (polling 아님)
```

### 캐싱
```
- 목적: 조회 성능 향상
- 구현: Spring Cache + Redis
- TTL:
  - popularProducts: 5분 (변동 적음)
  - productList: 30초 (재고 변동 빈번)
- 직렬화: JSON (@class 포함)
```

### Master-Replica
```
- Master (6379): 쓰기 전용
- Replica (6380): 읽기 전용
- 장점: 부하 분산 + 고가용성
``
