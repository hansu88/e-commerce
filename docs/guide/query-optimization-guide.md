# 효율적인 쿼리 최적화 실전 가이드

---

## 📋 개요

이 문서는 실전에서 쿼리를 최적화하는 구체적인 방법과 테크닉을 제공합니다.

---

## 1️⃣ EXPLAIN으로 쿼리 분석하기

### EXPLAIN 실행 방법

```sql
EXPLAIN SELECT * FROM products WHERE status = 'AVAILABLE';
```

### 주요 컬럼 해석

| 컬럼 | 의미 | 좋은 값 | 나쁜 값 |
|------|------|---------|---------|
| **type** | 접근 방식 | const, eq_ref, ref | ALL (Full Scan) |
| **key** | 사용된 인덱스 | 인덱스 이름 | NULL |
| **rows** | 예상 조회 행 수 | 적을수록 좋음 | 많으면 나쁨 |
| **Extra** | 추가 정보 | Using index | Using filesort |

---

### type 컬럼 상세

**성능 순위** (좋음 → 나쁨):
1. **const**: Primary Key or UNIQUE로 단 1건 조회
   ```sql
   SELECT * FROM products WHERE id = 1;
   ```

2. **eq_ref**: JOIN에서 Primary Key or UNIQUE 사용
   ```sql
   SELECT * FROM orders o
   JOIN users u ON o.user_id = u.id;
   ```

3. **ref**: 일반 인덱스 사용
   ```sql
   SELECT * FROM products WHERE status = 'AVAILABLE';
   -- idx_status 사용
   ```

4. **range**: 인덱스 범위 스캔
   ```sql
   SELECT * FROM orders
   WHERE created_at >= '2024-11-01' AND created_at < '2024-12-01';
   -- idx_created_at 사용
   ```

5. **index**: 인덱스 Full Scan
   ```sql
   SELECT id FROM products ORDER BY id;
   ```

6. **ALL**: Full Table Scan ❌
   ```sql
   SELECT * FROM products WHERE description LIKE '%검색어%';
   -- 인덱스 사용 불가
   ```

---

### Extra 컬럼 상세

**좋은 경우**:
- `Using index`: 커버링 인덱스 (인덱스만으로 조회) ✅
- `Using index condition`: 인덱스 조건 푸시다운 ✅

**나쁜 경우**:
- `Using filesort`: 정렬 시 임시 파일 사용 ⚠️
- `Using temporary`: 임시 테이블 사용 ⚠️
- `Using where`: WHERE 조건이 인덱스를 못 탐 ⚠️

---

### 실전 예시

#### ❌ 나쁜 쿼리
```sql
EXPLAIN SELECT * FROM order_items WHERE created_at > '2024-11-01';
```

| type | key | rows | Extra |
|------|-----|------|-------|
| ALL | NULL | 1,000,000 | Using where |

**문제**: Full Table Scan

---

#### ✅ 개선 후
```sql
-- 인덱스 추가
CREATE INDEX idx_created_at ON order_items(created_at);

EXPLAIN SELECT * FROM order_items WHERE created_at > '2024-11-01';
```

| type | key | rows | Extra |
|------|-----|------|-------|
| range | idx_created_at | 2,740 | Using index condition |

**개선**: Range Scan (365배 적은 행 조회)

---

## 2️⃣ 인덱스 설계 전략

### 인덱스가 필요한 곳

1. **WHERE 절 컬럼**
   ```sql
   SELECT * FROM orders WHERE status = 'PAID';
   -- CREATE INDEX idx_status ON orders(status);
   ```

2. **JOIN 키**
   ```sql
   SELECT * FROM orders o
   JOIN order_items oi ON o.id = oi.order_id;
   -- CREATE INDEX idx_order_id ON order_items(order_id);
   ```

3. **ORDER BY 컬럼**
   ```sql
   SELECT * FROM products ORDER BY created_at DESC;
   -- CREATE INDEX idx_created_at ON products(created_at);
   ```

4. **GROUP BY 컬럼**
   ```sql
   SELECT product_id, SUM(quantity) FROM order_items
   GROUP BY product_id;
   -- CREATE INDEX idx_product_id ON order_items(product_id);
   ```

---

### 복합 인덱스 설계

#### 규칙 1: 카디널리티 높은 컬럼을 앞에

```sql
-- ❌ 나쁨: status(4개 값) → user_id(100만 개 값)
CREATE INDEX idx_bad ON orders(status, user_id);

-- ✅ 좋음: user_id(100만 개 값) → status(4개 값)
CREATE INDEX idx_good ON orders(user_id, status);
```

**이유**: 선택도가 높은 컬럼이 앞에 있어야 범위를 빠르게 좁힘

---

#### 규칙 2: WHERE → ORDER BY 순서

```sql
-- 쿼리
SELECT * FROM orders
WHERE user_id = 1
ORDER BY created_at DESC;

-- ✅ 좋은 인덱스
CREATE INDEX idx_user_created ON orders(user_id, created_at);
```

**동작**:
1. `user_id = 1`로 범위 좁힘
2. 이미 `created_at` 순서로 정렬되어 있음 (filesort 불필요)

---

#### 규칙 3: 범위 조건은 마지막에

```sql
-- 쿼리
SELECT * FROM orders
WHERE user_id = 1
  AND status = 'PAID'
  AND created_at >= '2024-11-01';

-- ✅ 좋은 인덱스
CREATE INDEX idx_user_status_created ON orders(user_id, status, created_at);
```

**이유**: 범위 조건(`>=`) 이후 컬럼은 인덱스 사용 불가

---

### 인덱스가 사용되지 않는 경우

#### 1. 함수 사용
```sql
-- ❌ 인덱스 안 탐
SELECT * FROM orders WHERE DATE(created_at) = '2024-11-01';

-- ✅ 인덱스 탐
SELECT * FROM orders
WHERE created_at >= '2024-11-01 00:00:00'
  AND created_at < '2024-11-02 00:00:00';
```

---

#### 2. OR 조건
```sql
-- ❌ 인덱스 안 탐
SELECT * FROM products WHERE name = 'A' OR price = 1000;

-- ✅ UNION으로 변경
SELECT * FROM products WHERE name = 'A'
UNION
SELECT * FROM products WHERE price = 1000;
```

---

#### 3. LIKE '%검색어%'
```sql
-- ❌ 인덱스 안 탐 (앞에 %)
SELECT * FROM products WHERE name LIKE '%운동화%';

-- ✅ 인덱스 탐 (뒤에만 %)
SELECT * FROM products WHERE name LIKE '운동화%';
```

**해결**: Full-Text Search 또는 Elasticsearch 사용

---

#### 4. 타입 불일치
```sql
-- ❌ 인덱스 안 탐 (암시적 형변환)
SELECT * FROM orders WHERE user_id = '123';  -- user_id는 BIGINT

-- ✅ 인덱스 탐
SELECT * FROM orders WHERE user_id = 123;
```

---

## 3️⃣ JOIN 최적화

### JOIN 순서

**옵티마이저가 자동으로 최적 순서 선택** (보통)
- 작은 테이블 → 큰 테이블 순서로 JOIN

#### 강제 JOIN 순서 (필요 시)
```sql
SELECT STRAIGHT_JOIN o.*, oi.*
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE o.user_id = 1;
```

---

### JOIN vs Subquery

#### ❌ 나쁨: Subquery
```sql
SELECT *
FROM products
WHERE id IN (
    SELECT product_id FROM order_items WHERE quantity > 10
);
```

**문제**: Subquery가 매번 실행될 수 있음

---

#### ✅ 좋음: JOIN
```sql
SELECT DISTINCT p.*
FROM products p
JOIN order_items oi ON p.id = oi.product_id
WHERE oi.quantity > 10;
```

**장점**: 옵티마이저가 최적화 가능

---

### Nested Loop vs Hash Join

**Nested Loop Join** (기본):
- 작은 결과 집합에 적합
- 인덱스 활용

**Hash Join** (MySQL 8.0+):
- 큰 결과 집합에 적합
- 인덱스 없어도 빠름

```sql
-- Hash Join 힌트
SELECT /*+ HASH_JOIN(o, oi) */ *
FROM orders o
JOIN order_items oi ON o.id = oi.order_id;
```

---

## 4️⃣ 페이징 최적화

### 일반 페이징 (OFFSET)

```sql
-- ❌ 느림: OFFSET이 클수록 느려짐
SELECT * FROM products
ORDER BY id
LIMIT 10 OFFSET 100000;  -- 10만 번째부터 10개
```

**문제**: 10만 개를 읽고 버림

---

### 커서 기반 페이징 (No Offset)

```sql
-- ✅ 빠름: 마지막 ID 기준으로 조회
SELECT * FROM products
WHERE id > 100010  -- 이전 페이지 마지막 ID
ORDER BY id
LIMIT 10;
```

**장점**:
- OFFSET 없음
- 인덱스만 사용
- 페이지 번호 상관없이 빠름

---

### 실전 구현 (Spring Data JPA)

```java
// Repository
@Query("SELECT p FROM Product p WHERE p.id > :lastId ORDER BY p.id")
List<Product> findNextPage(@Param("lastId") Long lastId, Pageable pageable);

// Controller
public List<ProductDto> getProducts(Long lastId) {
    Pageable pageable = PageRequest.of(0, 10);
    List<Product> products = productRepository.findNextPage(lastId, pageable);
    // 클라이언트는 마지막 ID를 다음 요청에 전달
    return products.stream().map(ProductDto::from).toList();
}
```

---

## 5️⃣ COUNT 최적화

### COUNT(*) vs COUNT(column)

```sql
-- ✅ 빠름: COUNT(*)
SELECT COUNT(*) FROM products WHERE status = 'AVAILABLE';

-- ❌ 느림: COUNT(column)
SELECT COUNT(id) FROM products WHERE status = 'AVAILABLE';
```

**이유**: `COUNT(*)`는 인덱스만 사용 가능 (커버링 인덱스)

---

### COUNT 대신 EXISTS

```sql
-- ❌ 느림: 전체 개수 세기
SELECT COUNT(*) FROM orders WHERE user_id = 1;
boolean exists = count > 0;

-- ✅ 빠름: 존재 여부만 확인
SELECT EXISTS(SELECT 1 FROM orders WHERE user_id = 1 LIMIT 1);
```

**이유**: EXISTS는 첫 번째 발견 시 중단

---

### 대략적인 COUNT

```sql
-- 정확한 COUNT (느림)
SELECT COUNT(*) FROM orders;  -- 1억 건 스캔

-- 대략적인 COUNT (빠름)
SELECT table_rows FROM information_schema.tables
WHERE table_name = 'orders';
```

**용도**: 대시보드, 통계 (정확도 불필요)

---

## 6️⃣ INSERT/UPDATE 최적화

### Batch Insert

```java
// ❌ 나쁨: N번 INSERT
for (OrderItem item : items) {
    orderItemRepository.save(item);
}
// → 100개 → 100번 INSERT

// ✅ 좋음: 1번 Batch INSERT
orderItemRepository.saveAll(items);
// → 100개 → 1번 INSERT (또는 Batch 단위)
```

**설정** (`application.yml`):
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50  # Batch 크기
        order_inserts: true  # INSERT 순서 정렬
        order_updates: true  # UPDATE 순서 정렬
```

---

### Bulk Update

```java
// ❌ 나쁨: N번 UPDATE
for (UserCoupon uc : userCoupons) {
    uc.setStatus(UserCouponStatus.EXPIRED);
    userCouponRepository.save(uc);
}

// ✅ 좋음: 1번 Bulk UPDATE
@Modifying
@Query("UPDATE UserCoupon uc SET uc.status = :status WHERE uc.id IN :ids")
void updateStatusByIds(@Param("status") UserCouponStatus status,
                       @Param("ids") List<Long> ids);
```

**성능**: 5,000개 UPDATE → 12초 → 0.05초 (240배)

---

## 7️⃣ 실전 최적화 사례

### 사례 1: 인기 상품 조회

#### Before
```java
// UseCase
List<OrderItem> items = orderItemRepository.findAll();  // 100만 개

// Java Stream으로 집계
Map<Long, Integer> productSales = items.stream()
    .filter(item -> item.getCreatedAt().isAfter(yesterday))
    .collect(Collectors.groupingBy(
        OrderItem::getProductId,
        Collectors.summingInt(OrderItem::getQuantity)
    ));
```

**문제**: 메모리 500MB, 3.5초

---

#### After
```java
// Repository
@Query(value = "SELECT oi.product_id, SUM(oi.quantity) as total " +
        "FROM order_items oi " +
        "WHERE oi.created_at >= :startDate " +
        "GROUP BY oi.product_id " +
        "ORDER BY total DESC " +
        "LIMIT 10",
        nativeQuery = true)
List<Object[]> findTopProducts(@Param("startDate") LocalDateTime startDate);
```

**개선**: 메모리 1MB, 0.015초 (233배)

---

### 사례 2: 쿠폰 만료 처리

#### Before
```java
// 1. AVAILABLE 쿠폰 전체 조회
List<UserCoupon> userCoupons = userCouponRepository
    .findByStatus(UserCouponStatus.AVAILABLE);  // 5,000개

// 2. 각 쿠폰마다 Coupon 조회 (N+1)
for (UserCoupon uc : userCoupons) {
    Coupon coupon = couponRepository.findById(uc.getCouponId()).orElseThrow();
    if (coupon.getValidUntil().isBefore(now)) {
        uc.setStatus(UserCouponStatus.EXPIRED);
        userCouponRepository.save(uc);
    }
}
```

**문제**: 5,001번 쿼리, 12.5초

---

#### After
```java
// Repository - JOIN으로 한 번에 조회
@Query(value = "SELECT uc.* FROM user_coupons uc " +
        "INNER JOIN coupons c ON uc.coupon_id = c.id " +
        "WHERE uc.status = 'AVAILABLE' AND c.valid_until < :now",
        nativeQuery = true)
List<UserCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);

// UseCase
List<UserCoupon> expiredCoupons = userCouponRepository.findExpiredCoupons(now);
expiredCoupons.forEach(uc -> uc.setStatus(UserCouponStatus.EXPIRED));
userCouponRepository.saveAll(expiredCoupons);  // Batch Update
```

**개선**: 1번 쿼리, 0.025초 (500배)

---

### 사례 3: 상품 목록 조회

#### Before
```java
List<Product> products = productRepository.findAll();  // 100개

// N+1: 각 상품마다 옵션 조회
for (Product product : products) {
    List<ProductOption> options = productOptionRepository
        .findByProductId(product.getId());  // N번
    int totalStock = options.stream()
        .mapToInt(ProductOption::getStock)
        .sum();
}
```

**문제**: 101번 쿼리, 250ms

---

#### After
```java
@Query(value = "SELECT p.id, p.name, p.price, p.status, " +
        "COALESCE(SUM(po.stock), 0) as total_stock " +
        "FROM products p " +
        "LEFT JOIN product_options po ON p.id = po.product_id " +
        "GROUP BY p.id, p.name, p.price, p.status",
        nativeQuery = true)
List<Object[]> findAllWithTotalStockNative();
```

**개선**: 1번 쿼리, 2.5ms (100배)

---

## 8️⃣ 모니터링 및 디버깅

### Slow Query Log 활성화

```sql
-- Slow Query 로깅 (1초 이상)
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

**확인**:
```bash
tail -f /var/log/mysql/slow-query.log
```

---

### JPA 쿼리 로깅

```yaml
# application.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

### P6Spy로 실행 쿼리 확인

```gradle
// build.gradle
implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0'
```

```yaml
# application.yml
decorator:
  datasource:
    p6spy:
      enable-logging: true
```

**출력 예시**:
```
Hibernate: select o1_0.id, o1_0.user_id from orders o1_0 where o1_0.user_id=?
        binding parameter [1] as [BIGINT] - [1]
```

---

## 9️⃣ 성능 목표 설정

### 쿼리 실행 시간 목표

| 쿼리 유형 | 목표 시간 | 최대 허용 |
|----------|----------|----------|
| Primary Key 조회 | < 1ms | 10ms |
| 인덱스 조회 | < 10ms | 50ms |
| 집계 쿼리 | < 50ms | 200ms |
| Full Scan | 피할 것 | 1초 |

---

### 최적화 우선순위

1. **Full Table Scan 제거** (type=ALL)
2. **N+1 쿼리 제거**
3. **인덱스 추가** (WHERE, JOIN, ORDER BY)
4. **필요한 컬럼만 조회** (SELECT *)
5. **Batch 처리** (INSERT, UPDATE)

---

## 🔟 체크리스트

쿼리 최적화 전 확인:

- [ ] EXPLAIN으로 실행 계획 확인했는가?
- [ ] type이 ALL (Full Scan)인가?
- [ ] key가 NULL (인덱스 안 탐)인가?
- [ ] Extra에 Using filesort/temporary 있는가?
- [ ] N+1 문제 없는가?
- [ ] 필요한 컬럼만 조회하는가?
- [ ] WHERE, JOIN 컬럼에 인덱스 있는가?
- [ ] 함수 사용으로 인덱스 무효화되지 않았는가?
- [ ] 실행 시간이 목표치 이내인가?

---

## 🎯 결론

### 핵심 최적화 원칙
1. **EXPLAIN으로 분석** (type, key, rows)
2. **인덱스 설계** (WHERE, JOIN, ORDER BY)
3. **JOIN 활용** (N+1 방지)
4. **Batch 처리** (INSERT, UPDATE)
5. **커버링 인덱스** (SELECT 컬럼 최소화)

### 성능 목표
- 단일 조회: < 10ms
- 목록 조회: < 50ms
- 집계 쿼리: < 200ms

### 모니터링
- Slow Query Log
- JPA 쿼리 로깅
- P6Spy

### 실전 개선 효과
- 쿼리 수: 평균 99% 감소
- 실행 시간: 평균 200배 개선
- 메모리: 평균 99% 감소
