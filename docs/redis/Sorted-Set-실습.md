# Redis Sorted Set 실습 가이드

## 🎯 Sorted Set이란?

**점수(score)와 함께 저장되는 정렬된 집합**
- 자동으로 점수 순 정렬
- 중복 없음 (같은 member는 1개만)
- 랭킹, 리더보드에 최적화

---

## 📊 실생활 비유

### 게임 리더보드
```
[ 1위: 철수  1000점 ] ← 가장 높은 점수
[ 2위: 영희   950점 ]
[ 3위: 민수   800점 ]
[ 4위: 지수   750점 ]

특징:
✅ 점수가 바뀌면 자동으로 순위 재정렬
✅ 누가 몇 등인지 빠르게 조회
✅ TOP 10, TOP 100 쉽게 조회
```

### 우리 프로젝트: 상품 랭킹
```
[ 1위: 상품 5번  120개 판매 ]
[ 2위: 상품 3번   95개 판매 ]
[ 3위: 상품 1번   80개 판매 ]

Redis Sorted Set:
- Key: "product:ranking"
- Score: 판매 수량 (누적)
- Member: 상품 ID
```

---

## 🔧 Redis CLI 실습

### 1. 기본 명령어

#### ZADD - 데이터 추가
```bash
# 형식: ZADD key score member
ZADD product:ranking 50 "5"     # 상품 5번, 50개 판매
ZADD product:ranking 42 "3"     # 상품 3번, 42개 판매
ZADD product:ranking 38 "1"     # 상품 1번, 38개 판매
ZADD product:ranking 25 "7"     # 상품 7번, 25개 판매
ZADD product:ranking 18 "9"     # 상품 9번, 18개 판매

# 결과: 자동으로 점수 순 정렬됨
```

#### ZINCRBY - 점수 증가
```bash
# 형식: ZINCRBY key increment member
ZINCRBY product:ranking 10 "3"  # 상품 3번에 10개 추가
# → 42 + 10 = 52

ZINCRBY product:ranking 5 "1"   # 상품 1번에 5개 추가
# → 38 + 5 = 43

# 주문 시마다 이 명령어로 점수 누적!
```

#### ZREVRANGE - TOP N 조회 (높은 점수 순)
```bash
# 형식: ZREVRANGE key start stop [WITHSCORES]
ZREVRANGE product:ranking 0 4 WITHSCORES  # TOP 5 조회

# 결과:
1) "3"      # 1위: 상품 3번
2) "52"     # 점수: 52
3) "5"      # 2위: 상품 5번
4) "50"     # 점수: 50
5) "1"      # 3위: 상품 1번
6) "43"     # 점수: 43
7) "7"      # 4위: 상품 7번
8) "25"     # 점수: 25
9) "9"      # 5위: 상품 9번
10) "18"    # 점수: 18
```

#### ZREVRANK - 순위 확인
```bash
# 형식: ZREVRANK key member
ZREVRANK product:ranking "3"    # 상품 3번의 순위
# → 0 (1등, 인덱스는 0부터 시작)

ZREVRANK product:ranking "5"
# → 1 (2등)

ZREVRANK product:ranking "9"
# → 4 (5등)
```

#### ZSCORE - 점수 조회
```bash
# 형식: ZSCORE key member
ZSCORE product:ranking "3"      # 상품 3번의 점수
# → 52.0

ZSCORE product:ranking "999"    # 없는 상품
# → (nil)
```

---

## 2. 고급 명령어

#### ZCARD - 전체 개수
```bash
ZCARD product:ranking
# → 5 (5개 상품이 랭킹에 있음)
```

#### ZREM - 데이터 삭제
```bash
ZREM product:ranking "9"        # 상품 9번 삭제
# → 1 (삭제 성공)

ZREM product:ranking "999"      # 없는 상품 삭제
# → 0 (삭제 실패)
```

#### ZRANGE - 낮은 점수 순 조회
```bash
ZRANGE product:ranking 0 2 WITHSCORES  # 최하위 3개
# → 점수가 낮은 순서대로 (판매 안 되는 상품)
```

---

## 💻 Spring Boot 코드 예시

### RedisTemplate 사용

```java
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RANKING_KEY = "product:ranking";

    /**
     * 상품 판매 시 점수 증가
     */
    public void incrementProductScore(Long productId, int quantity) {
        redisTemplate.opsForZSet()
            .incrementScore(RANKING_KEY, productId.toString(), quantity);
    }

    /**
     * TOP N 상품 조회
     */
    public List<ProductRankingDto> getTopProducts(int count) {
        // 0부터 count-1까지 (예: TOP 5 = 0~4)
        Set<ZSetOperations.TypedTuple<String>> topSet =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANKING_KEY, 0, count - 1);

        if (topSet == null) {
            return Collections.emptyList();
        }

        // DTO 변환
        return topSet.stream()
            .map(tuple -> new ProductRankingDto(
                Long.parseLong(tuple.getValue()),      // 상품 ID
                tuple.getScore().intValue()            // 판매 수량
            ))
            .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 점수 조회
     */
    public Integer getProductScore(Long productId) {
        Double score = redisTemplate.opsForZSet()
            .score(RANKING_KEY, productId.toString());

        return score != null ? score.intValue() : 0;
    }

    /**
     * 특정 상품의 순위 조회
     */
    public Integer getProductRank(Long productId) {
        Long rank = redisTemplate.opsForZSet()
            .reverseRank(RANKING_KEY, productId.toString());

        // rank는 0부터 시작 (0 = 1등)
        return rank != null ? rank.intValue() + 1 : null;
    }
}
```

---

## 🎓 핵심 개념 정리

### 1. 점수 vs 순위
```
점수 (Score): 실제 판매 수량 (예: 52개)
순위 (Rank): 몇 등인지 (예: 1등)

ZSCORE → 점수 조회
ZREVRANK → 순위 조회
```

### 2. 오름차순 vs 내림차순
```
ZRANGE → 낮은 점수 순 (ASC)
ZREVRANGE → 높은 점수 순 (DESC) ⭐ 랭킹은 이걸 써야 함!

ZRANK → 낮은 점수 기준 순위
ZREVRANK → 높은 점수 기준 순위 ⭐ 랭킹은 이걸 써야 함!
```

### 3. 인덱스 시작
```
Redis 순위는 0부터 시작:
- 0 = 1등
- 1 = 2등
- 2 = 3등

사용자에게 보여줄 때는 +1 필요!
```

---

## 🧪 직접 실습해보기

### Docker Redis에 접속
```bash
docker exec -it redis-master redis-cli
```

### 실습 시나리오
```bash
# 1. 초기 데이터 입력
ZADD product:ranking 10 "1" 20 "2" 30 "3" 40 "4" 50 "5"

# 2. 상품 3번이 10개 더 팔림
ZINCRBY product:ranking 10 "3"

# 3. TOP 3 조회
ZREVRANGE product:ranking 0 2 WITHSCORES

# 4. 상품 3번의 순위는?
ZREVRANK product:ranking "3"

# 5. 상품 1번의 점수는?
ZSCORE product:ranking "1"

# 6. 전체 몇 개?
ZCARD product:ranking

# 7. 상품 1번 삭제
ZREM product:ranking "1"

# 8. 다시 TOP 3 조회
ZREVRANGE product:ranking 0 2 WITHSCORES
```

---

## ⚠️ 주의사항

### 1. Score는 실수형 (Double)
```java
// ❌ 잘못된 사용
int score = tuple.getScore();  // 컴파일 에러

// ✅ 올바른 사용
Double score = tuple.getScore();
int intScore = score.intValue();
```

### 2. Member는 문자열
```java
// Redis는 모든 값을 String으로 저장
redisTemplate.opsForZSet()
    .incrementScore(RANKING_KEY, productId.toString(), quantity);
    //                           ↑ toString() 필수!
```

### 3. null 체크 필수
```java
// reverseRangeWithScores는 null 반환 가능
Set<TypedTuple<String>> result = zSetOps.reverseRangeWithScores(...);
if (result == null) {
    return Collections.emptyList();
}
```

### 4. 순위는 0부터 시작
```java
// Redis rank: 0, 1, 2, ...
// 사용자에게 보여줄 때: 1, 2, 3, ...

Long rank = zSetOps.reverseRank(key, member);
int displayRank = rank != null ? rank.intValue() + 1 : 0;
```

---

## 성능 특성

### 시간 복잡도
```
ZADD: O(log N)        - 빠름
ZINCRBY: O(log N)     - 빠름
ZREVRANGE: O(log N + M)  - M은 조회 개수 (TOP 10이면 빠름)
ZREVRANK: O(log N)    - 빠름
ZSCORE: O(1)          - 매우 빠름
```

### DB vs Redis Sorted Set
```
DB (GROUP BY + ORDER BY):
- 매번 전체 주문 데이터 집계
- 느림 (수백 ms ~ 초)

Redis Sorted Set:
- 실시간 점수 업데이트
- 조회 빠름 (수 ms)
- 메모리 사용 (but 적음)
```

---
### 언제 업데이트?
```
주문 완료 시 (CreateOrderUseCase):
1. 주문 저장 (DB)
2. 재고 차감 (DB)
3. 랭킹 업데이트 (Redis) ← 여기!
   → rankingService.incrementProductScore(productId, quantity)
```

### 언제 조회?
```
상품 랭킹 API:
GET /api/products/ranking?count=5

→ rankingService.getTopProducts(5)
```

### TTL 설정은?
```
랭킹은 계속 누적되므로 TTL 불필요
- 주문 취소 시에도 점수 유지 (누적 판매량)
- 필요하면 스케줄러로 주기적 초기화 (예: 매월 1일)
```