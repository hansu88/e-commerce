# Redis 락 전략 비교: Spin Lock vs Pub/Sub Lock

## 🎯 개요

분산락 구현 시 2가지 대기 방식이 있습니다:
1. **Spin Lock (폴링)** - 계속 확인
2. **Pub/Sub Lock (이벤트)** - 알림 받기 ⭐ (Redisson 기본)

---

## 1️⃣ Spin Lock (스핀 락)

### 동작 방식
```
Thread-2가 락 획득 시도
   ↓
락이 있나? → 없음 (실패)
   ↓
100ms 대기
   ↓
다시 확인: 락이 있나? → 없음 (실패)
   ↓
100ms 대기
   ↓
다시 확인: 락이 있나? → 없음 (실패)
   ↓
... (반복)
```

### 코드 예시
```java
while (!lock.tryLock()) {
    Thread.sleep(100); // 계속 대기하면서 확인
}
```

### 장점
- ✅ 구현 간단
- ✅ Redis 기능 적게 사용 (EXISTS만)

### 단점
- ❌ **CPU 낭비**: 계속 Redis 쿼리 날림
- ❌ **네트워크 부하**: 100ms마다 Redis 통신
- ❌ **불필요한 확인**: 락이 해제되기 전에도 계속 확인
- ❌ **대기 시간 불확실**: 언제 해제될지 모름

---

## 2️⃣ Pub/Sub Lock (펍섭 락) ⭐

### 동작 방식
```
Thread-2가 락 획득 시도
   ↓
락이 있나? → 있음 (실패)
   ↓
Pub/Sub 채널 구독 (대기)
   ↓
(Thread-1이 unlock)
   ↓
PUBLISH unlock 메시지 → Thread-2 깨어남!
   ↓
즉시 락 획득 시도 → 성공!
```

### 코드 예시 (Redisson 내부)
```java
// Thread-1: unlock 시
redis.publish("redisson_lock__channel:order:user:1", "unlock");

// Thread-2: 대기 중
redis.subscribe("redisson_lock__channel:order:user:1");
// 메시지 받으면 깨어남
```

### 장점
- ✅ **CPU 효율적**: 대기 중 아무것도 안 함 (sleep)
- ✅ **네트워크 절약**: unlock 시 1번만 통신
- ✅ **빠른 반응**: 락 해제되자마자 즉시 알림
- ✅ **확장성**: 여러 스레드가 대기해도 괜찮음

### 단점
- ❌ 구현 복잡 (Redisson이 다 해줌)
- ❌ Redis Pub/Sub 기능 필요

---

## 3️⃣ 성능 비교 (이론)

### 시나리오: 5개 스레드가 순차 대기

| 항목 | Spin Lock | Pub/Sub Lock |
|------|-----------|--------------|
| **Redis 쿼리 수** | 100회+ (계속 확인) | 5회 (unlock 알림) |
| **CPU 사용률** | 높음 (busy waiting) | 낮음 (event driven) |
| **네트워크 부하** | 높음 (반복 쿼리) | 낮음 (알림만) |
| **평균 대기 시간** | 불확실 (100ms 간격) | 즉시 (~1ms) |
| **확장성** | 나쁨 (스레드↑ = 부하↑) | 좋음 (이벤트 방식) |

### 구체적 예시

**Spin Lock:**
```
Thread-1: 0ms  락 획득 ──────────────── 1500ms 해제
Thread-2: 0ms  대기 (100ms마다 확인)
           ↓
         100ms  확인 → 실패
         200ms  확인 → 실패
         300ms  확인 → 실패
         ...
        1500ms  확인 → 실패
        1600ms  확인 → 성공! (락 획득)

→ 1600ms 동안 16번 Redis 쿼리
→ 100ms 간격이라 1500ms에 해제돼도 1600ms에 알게 됨
```

**Pub/Sub Lock:**
```
Thread-1: 0ms  락 획득 ──────────────── 1500ms 해제 (PUBLISH)
Thread-2: 0ms  대기 (구독 중, sleep)
           ↓
        1500ms  unlock 알림 받음 (즉시 깨어남!)
        1501ms  락 획득 성공!

→ 1501ms (거의 즉시)
→ Redis 쿼리 1번 (PUBLISH)
```

---

## 4️⃣ 우리 프로젝트 선택: Pub/Sub Lock

### 왜 Pub/Sub를 선택했나?

1. **Redisson 기본 제공**
   - 별도 구현 불필요
   - 검증된 라이브러리

2. **성능 우수**
   - 주문 처리 시 여러 스레드 대기 가능
   - Spin Lock보다 CPU 효율 좋음

3. **확장성**
   - 트래픽 증가해도 성능 유지
   - Redis 부하 적음

### Redisson 내부 동작

```java
// tryLock() 내부 (단순화)
public boolean tryLock(long waitTime, TimeUnit unit) {
    // 1. 락 획득 시도
    if (tryAcquire()) {
        return true;
    }

    // 2. 실패 시 Pub/Sub 구독
    RFuture<RedissonLockEntry> future = subscribe();

    // 3. unlock 알림 대기
    Semaphore semaphore = future.get().getLatch();
    semaphore.tryAcquire(waitTime, unit);

    // 4. 깨어나서 다시 시도
    return tryAcquire();
}
```

---

## 5️⃣ 실무 선택 가이드

### Spin Lock을 쓸 때
- ✅ 락 보유 시간이 **매우 짧음** (수 ms)
- ✅ 대기 스레드가 **1~2개**
- ✅ Redis Pub/Sub 사용 불가

### Pub/Sub Lock을 쓸 때 ⭐
- ✅ 락 보유 시간이 **김** (수백 ms ~ 초)
- ✅ 대기 스레드가 **많음** (3개 이상)
- ✅ 성능이 중요
- ✅ Redis Pub/Sub 사용 가능 (일반적)

**→ 대부분의 경우 Pub/Sub Lock이 유리!**

---

## 6️⃣ 성능 측정 방법 (추후)

### 측정 항목
```java
// 1. Redis 쿼리 수
long queryCount = redisMonitor.getQueryCount();

// 2. CPU 사용률
double cpuUsage = systemMonitor.getCpuUsage();

// 3. 평균 대기 시간
long avgWaitTime = lockMetrics.getAvgWaitTime();

// 4. 락 충돌 횟수
long lockContentions = lockMetrics.getContentions();
```

### JMH 벤치마크 예시
```java
@Benchmark
public void spinLockBenchmark() {
    // Spin Lock으로 1000번 락 획득/해제
}

@Benchmark
public void pubSubLockBenchmark() {
    // Pub/Sub Lock으로 1000번 락 획득/해제
}
```

---
