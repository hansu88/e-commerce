# Redis 선착순 처리 실습 가이드

## 🎯 선착순 쿠폰 발급이란?

**한정된 수량의 쿠폰을 먼저 신청한 순서대로 지급**
- 수량: 100장 한정
- 규칙: 1인당 1장만
- 조건: 선착순 마감

---

## 📊 실생활 비유

### 콘서트 티켓팅
```
[ 좌석 100석 ]

10:00:00 티켓 오픈
10:00:01 → 1만 명 동시 접속!

❌ 잘못된 방식 (DB만 사용):
1만 명 모두 DB에 쿼리 → DB 다운 💥

✅ 올바른 방식 (Redis 선착순):
Redis에서 100명만 통과 → 나머지 9,900명은 즉시 실패
→ DB에는 100개 요청만!
```

### 우리 프로젝트: 쿠폰 발급
```
[ 쿠폰 10장 ]

100명이 동시에 신청

Redis 선착순:
1. Redis에서 빠르게 카운트 (INCR)
2. 10명만 통과 → DB 저장
3. 나머지 90명 즉시 실패 → DB 접근 안 함!
```

---

## 🔧 Redis 명령어

### 1. INCR - 원자적 증가

```bash
# 초기값 설정
SET coupon:issued:1 0

# 증가 (원자적!)
INCR coupon:issued:1   # → 1
INCR coupon:issued:1   # → 2
INCR coupon:issued:1   # → 3

# 조회
GET coupon:issued:1    # → "3"

# 특징: 동시에 100개 INCR 해도 정확히 100!
```

#### 왜 원자적인가?
```
Redis는 싱글 스레드:
명령어를 한 번에 하나씩만 처리

Thread-1: INCR (읽기 0 → 쓰기 1)
Thread-2: INCR (읽기 1 → 쓰기 2)  ← Thread-1 완료 후!
Thread-3: INCR (읽기 2 → 쓰기 3)

→ Race Condition 없음!
```

---

### 2. SADD - Set에 추가 (중복 방지)

```bash
# Set 생성 및 추가
SADD coupon:users:1 "user:123"  # → 1 (추가 성공)
SADD coupon:users:1 "user:456"  # → 1 (추가 성공)
SADD coupon:users:1 "user:123"  # → 0 (이미 있음, 중복!)

# 존재 여부 확인
SISMEMBER coupon:users:1 "user:123"  # → 1 (있음)
SISMEMBER coupon:users:1 "user:999"  # → 0 (없음)

# Set 크기 확인
SCARD coupon:users:1  # → 2 (user:123, user:456)

# 전체 조회
SMEMBERS coupon:users:1  # → ["user:123", "user:456"]
```

#### 중복 발급 방지
```
사용자 123이 두 번 클릭:

1번째: SADD → 1 (성공) → 쿠폰 발급
2번째: SADD → 0 (실패) → 이미 발급받음!
```

---

## 💻 Spring Boot 코드 예시

### Redis에서 선착순 체크

```java
@Service
@RequiredArgsConstructor
public class CouponService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 선착순 쿠폰 발급 가능 여부 체크
     *
     * @return true: 발급 가능, false: 한도 초과
     */
    public boolean tryIssueCoupon(Long couponId, Long userId, int totalLimit) {
        String issuedKey = "coupon:issued:" + couponId;
        String usersKey = "coupon:users:" + couponId;
        String userValue = "user:" + userId;

        // 1. 중복 발급 체크
        Boolean alreadyIssued = redisTemplate.opsForSet()
            .isMember(usersKey, userValue);

        if (Boolean.TRUE.equals(alreadyIssued)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 2. 발급 수량 증가 (원자적)
        Long issuedCount = redisTemplate.opsForValue()
            .increment(issuedKey, 1);

        // 3. 한도 체크
        if (issuedCount == null || issuedCount > totalLimit) {
            // 한도 초과 시 롤백 (감소)
            redisTemplate.opsForValue().decrement(issuedKey, 1);
            return false;
        }

        // 4. 사용자 Set에 추가 (중복 방지)
        redisTemplate.opsForSet().add(usersKey, userValue);

        return true;
    }

    /**
     * 현재 발급 수량 조회
     */
    public int getIssuedCount(Long couponId) {
        String key = "coupon:issued:" + couponId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 남은 수량 조회
     */
    public int getRemainingCount(Long couponId, int totalLimit) {
        int issued = getIssuedCount(couponId);
        return Math.max(totalLimit - issued, 0);
    }
}
```

---

## 🎓 핵심 개념 정리

### 1. 왜 Redis를 사용하나?

| 비교 | DB (비관적 락) | Redis (INCR) |
|------|---------------|--------------|
| **속도** | 느림 (디스크 I/O) | 빠름 (메모리) |
| **부하** | DB에 집중 | Redis로 분산 |
| **동시성** | 락 대기 (순차) | 원자적 처리 |
| **한도 초과 시** | DB 쿼리 후 실패 | Redis에서 즉시 실패 |

---

### 2. 흐름 비교

#### Before (DB만 사용)
```
100명 신청 (한도 10장)

모두 DB 접근:
1. SELECT ... FOR UPDATE  ← 락 대기
2. 재고 체크
3. INSERT
4. 커밋

→ 100번 DB 쿼리! 😰
```

#### After (Redis + DB)
```
100명 신청 (한도 10장)

1. Redis 선착순 체크 (빠름!)
   - 10명 통과 → DB 저장
   - 90명 실패 → 즉시 리턴

→ 10번만 DB 쿼리! 😎
```

---

### 3. Redis 키 설계

```
coupon:issued:{couponId}  → 발급 수량 (String, INCR)
- 예: coupon:issued:1 = "15"

coupon:users:{couponId}   → 발급 받은 사용자 (Set)
- 예: coupon:users:1 = {"user:1", "user:5", "user:10", ...}

coupon:limit:{couponId}   → 총 한도 (String, 선택)
- 예: coupon:limit:1 = "100"
```

---

## 🧪 직접 실습해보기

### Docker Redis에 접속
```bash
docker exec -it redis-master redis-cli
```

### 시나리오: 쿠폰 10장, 3명 신청

```bash
# 1. 초기 설정
SET coupon:issued:1 0
SET coupon:limit:1 10

# 2. 사용자 1번 신청
INCR coupon:issued:1             # → 1 (통과!)
SADD coupon:users:1 "user:1"     # → 1 (추가)

# 3. 사용자 1번 다시 신청 (중복)
SISMEMBER coupon:users:1 "user:1"  # → 1 (이미 있음!)
# → "이미 발급받은 쿠폰입니다" 에러!

# 4. 사용자 2번 신청
INCR coupon:issued:1             # → 2 (통과!)
SADD coupon:users:1 "user:2"     # → 1 (추가)

# 5. 사용자 3번 신청
INCR coupon:issued:1             # → 3 (통과!)
SADD coupon:users:1 "user:3"     # → 1 (추가)

# 6. 현재 상태 확인
GET coupon:issued:1              # → "3"
SCARD coupon:users:1             # → 3
SMEMBERS coupon:users:1          # → ["user:1", "user:2", "user:3"]

# 7. 남은 수량
GET coupon:limit:1               # → "10"
GET coupon:issued:1              # → "3"
# 계산: 10 - 3 = 7장 남음
```

---

### 시나리오 2: 한도 초과

```bash
# 쿠폰 2번, 한도 5장만
SET coupon:issued:2 0
SET coupon:limit:2 5

# 5명 발급
INCR coupon:issued:2  # → 1
INCR coupon:issued:2  # → 2
INCR coupon:issued:2  # → 3
INCR coupon:issued:2  # → 4
INCR coupon:issued:2  # → 5

# 6번째 사람 신청
INCR coupon:issued:2  # → 6
GET coupon:limit:2    # → "5"
# 체크: 6 > 5 → 한도 초과!

# 롤백 (옵션)
DECR coupon:issued:2  # → 5 (원복)
```

---

## ⚠️ 주의사항

### 1. 한도 초과 시 롤백

```java
// ❌ 잘못된 예
Long count = redisTemplate.opsForValue().increment(key, 1);
if (count > limit) {
    return false;  // count는 이미 증가됨! (limit + 1)
}

// ✅ 올바른 예
Long count = redisTemplate.opsForValue().increment(key, 1);
if (count > limit) {
    redisTemplate.opsForValue().decrement(key, 1);  // 롤백!
    return false;
}
```

### 2. Redis와 DB 정합성

```
문제 상황:
1. Redis 통과 (count = 5)
2. DB 저장 실패 (네트워크 오류)
3. Redis는 5인데, DB는 4개만 있음!

해결 방법:
1. 트랜잭션 후 Redis 증가 (권장)
   - DB 저장 성공 → Redis 증가
   - DB 저장 실패 → Redis 증가 안 함

2. 스케줄러로 정합성 체크
   - 매시간 Redis vs DB 비교
   - 차이 나면 알림
```

### 3. TTL 설정 (선택)

```bash
# 쿠폰 발급 종료 시간 설정 (7일 후 자동 삭제)
EXPIRE coupon:issued:1 604800    # 7일 = 604800초
EXPIRE coupon:users:1 604800

# 남은 시간 확인
TTL coupon:issued:1  # → 604799, 604798, ...
```

---

## 📈 성능 비교

### Before (DB 비관적 락)
```
100명 동시 신청 (한도 10장)

DB 쿼리: 100번
평균 응답 시간: 500ms (락 대기)
```

### After (Redis 선착순)
```
100명 동시 신청 (한도 10장)

Redis 체크: 100번 (빠름!)
DB 쿼리: 10번 (통과한 사람만)
평균 응답 시간: 50ms (10배 빠름!)
```

---

## 🎯 우리 프로젝트 적용 계획

### 언제 사용?
```
POST /api/coupons/issue

1. Redis 선착순 체크 (CouponService)
   - 중복 발급 체크
   - 발급 수량 증가
   - 한도 체크

2. 통과 시 DB 저장 (IssueCouponUseCase)
   - UserCoupon INSERT
   - Coupon.issuedQuantity 증가 (sync)

3. 실패 시 즉시 리턴
   - "쿠폰이 모두 소진되었습니다"
```

### DB와 동기화
```java
// DB의 issuedQuantity도 증가 (정합성 유지)
coupon.increaseIssuedQuantity();
couponRepository.save(coupon);

// 또는 스케줄러로 주기적 sync
```

---

## 📚 참고 자료

- [Redis INCR 공식 문서](https://redis.io/commands/incr/)
- [Redis SET 공식 문서](https://redis.io/commands/sadd/)
- [선착순 쿠폰 시스템 아키텍처](https://techblog.woowahan.com/2631/)
