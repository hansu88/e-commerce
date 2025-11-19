# Repository 쿼리 작성 가이드

---

## 📋 개요

이 문서는 Repository에서 효율적이고 안전한 쿼리를 작성하기 위한 가이드라인입니다.

---

## ❌ 나쁜 예시들

### 1. SELECT * 사용

```java
// ❌ 나쁜 예: 모든 컬럼 조회
@Query("SELECT p FROM Product p")
List<Product> findAll();

// SQL 결과
SELECT id, name, price, description, category, brand,
       image_url, status, created_at, updated_at, ...
FROM products;
```

**문제점**:
- 불필요한 컬럼까지 조회 (description, image_url 등)
- 네트워크 트래픽 증가
- 메모리 낭비
- 인덱스 커버링 불가

---

### 2. N+1 쿼리 문제

```java
// ❌ 나쁜 예: N+1 문제
@Query("SELECT p FROM Product p")
List<Product> findAll();

// UseCase에서
for (Product product : products) {
    // 각 상품마다 쿼리 실행! (N+1)
    int stock = productOptionRepository.findByProductId(product.getId())
        .stream()
        .mapToInt(ProductOption::getStock)
        .sum();
}
```

**결과**: 1 + N개 쿼리 실행

---

### 3. 메모리에서 필터링

```java
// ❌ 나쁜 예: 전체 조회 후 메모리에서 필터링
@Query("SELECT o FROM OrderItem o")
List<OrderItem> findAll();

// UseCase에서
List<OrderItem> filtered = orderItemRepository.findAll().stream()
    .filter(item -> item.getCreatedAt().isAfter(startDate))
    .toList();
```

**문제점**:
- 100만개 데이터를 전부 메모리에 로드
- OutOfMemoryError 위험
- DB 필터링이 훨씬 빠름

---

### 4. JOIN 없이 반복 조회

```java
// ❌ 나쁜 예: JOIN 없이 각각 조회
@Query("SELECT uc FROM UserCoupon uc WHERE uc.status = :status")
List<UserCoupon> findByStatus(@Param("status") UserCouponStatus status);

// UseCase에서
for (UserCoupon uc : userCoupons) {
    // 각 쿠폰마다 Coupon 조회
    Coupon coupon = couponRepository.findById(uc.getCouponId()).orElseThrow();
    // ...
}
```

**결과**: 5,000개 쿠폰 → 5,001개 쿼리

---

## ✅ 좋은 예시들

### 1. 필요한 컬럼만 조회 (DTO Projection)

```java
// ✅ 좋은 예: 필요한 컬럼만 선택
@Query(value = "SELECT p.id, p.name, p.price, p.status, " +
        "COALESCE(SUM(po.stock), 0) as total_stock " +
        "FROM products p " +
        "LEFT JOIN product_options po ON p.id = po.product_id " +
        "GROUP BY p.id, p.name, p.price, p.status",
        nativeQuery = true)
List<Object[]> findAllWithTotalStockNative();
```

**장점**:
- 필요한 5개 컬럼만 조회
- 네트워크 트래픽 최소화
- 인덱스 커버링 가능

**언제 사용**:
- 목록 조회 (리스트 페이지)
- 통계/집계 쿼리
- API 응답용 데이터

---

### 2. JOIN으로 N+1 해결

```java
// ✅ 좋은 예: 단일 쿼리로 JOIN
@Query(value = "SELECT uc.* FROM user_coupons uc " +
        "INNER JOIN coupons c ON uc.coupon_id = c.id " +
        "WHERE uc.status = 'AVAILABLE' AND c.valid_until < :now",
        nativeQuery = true)
List<UserCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);
```

**장점**:
- 5,001개 쿼리 → 1개 쿼리
- DB에서 JOIN 처리 (빠름)
- 네트워크 왕복 1회

**쿼리 수**: 5,001개 → 1개 (99.98% 감소)

---

### 3. WHERE 절로 DB 필터링

```java
// ✅ 좋은 예: DB에서 필터링
@Query("SELECT oi FROM OrderItem oi " +
        "WHERE oi.createdAt >= :startDateTime " +
        "AND oi.createdAt < :endDateTime")
List<OrderItem> findByCreatedAtBetween(
    @Param("startDateTime") LocalDateTime startDateTime,
    @Param("endDateTime") LocalDateTime endDateTime
);
```

**장점**:
- DB에서 필터링 (인덱스 활용)
- 1,000,000개 → 2,740개만 조회
- 메모리 사용량 99.7% 감소

**인덱스 필수**:
```java
@Table(name = "order_items",
    indexes = @Index(name = "idx_created_at", columnList = "created_at")
)
```

---

### 4. Fetch Join (즉시 로딩)

```java
// ✅ 좋은 예: Fetch Join으로 한 번에 조회
@Query("SELECT uc FROM UserCoupon uc " +
        "JOIN FETCH uc.coupon c " +
        "WHERE uc.userId = :userId")
List<UserCoupon> findByUserIdWithCoupon(@Param("userId") Long userId);
```

**언제 사용**:
- 연관된 엔티티를 항상 함께 사용하는 경우
- 상세 조회 (Detail 페이지)

**주의**: Fetch Join은 컬렉션에 사용 시 페이징 불가

---

## 📝 쿼리 작성 규칙

### 규칙 1: 필요한 컬럼만 조회

```java
// ❌ 나쁨
SELECT * FROM products;

// ✅ 좋음
SELECT id, name, price, status FROM products;
```

**예외**: 엔티티 전체가 필요한 경우 (UPDATE, 상세 조회)

---

### 규칙 2: WHERE 절 활용

```java
// ❌ 나쁨: 메모리 필터링
List<Order> all = orderRepository.findAll();
List<Order> filtered = all.stream()
    .filter(o -> o.getStatus() == OrderStatus.PAID)
    .toList();

// ✅ 좋음: DB 필터링
@Query("SELECT o FROM Order o WHERE o.status = :status")
List<Order> findByStatus(@Param("status") OrderStatus status);
```

---

### 규칙 3: JOIN 활용

```java
// ❌ 나쁨: 반복 조회
for (OrderItem item : items) {
    Product product = productRepository.findById(item.getProductId()).orElseThrow();
}

// ✅ 좋음: JOIN
@Query("SELECT oi, p FROM OrderItem oi " +
        "JOIN Product p ON oi.productId = p.id " +
        "WHERE oi.orderId = :orderId")
List<Object[]> findWithProduct(@Param("orderId") Long orderId);
```

---

### 규칙 4: 인덱스 활용

```java
// WHERE, JOIN, ORDER BY 컬럼에 인덱스 필수
@Table(name = "orders",
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_status", columnList = "status")
    }
)
```

**인덱스가 필요한 경우**:
- WHERE 절 조건
- JOIN 키
- ORDER BY 컬럼
- GROUP BY 컬럼

---

### 규칙 5: COUNT 대신 EXISTS 사용

```java
// ❌ 나쁨: 전체 개수 세기
@Query("SELECT COUNT(c) FROM Coupon c WHERE c.code = :code")
long countByCode(@Param("code") String code);

boolean exists = countByCode(code) > 0;  // 비효율

// ✅ 좋음: 존재 여부만 확인
@Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
        "FROM Coupon c WHERE c.code = :code")
boolean existsByCode(@Param("code") String code);
```

**이유**: EXISTS는 첫 번째 발견 시 중단

---

## 🎯 쿼리 타입별 가이드

### 목록 조회 (List)

```java
// Native Query + DTO Projection
@Query(value = "SELECT p.id, p.name, p.price " +
        "FROM products p " +
        "WHERE p.status = :status " +
        "ORDER BY p.created_at DESC " +
        "LIMIT :limit",
        nativeQuery = true)
List<Object[]> findRecentProducts(
    @Param("status") String status,
    @Param("limit") int limit
);
```

**포인트**:
- 필요한 컬럼만
- WHERE, ORDER BY, LIMIT 활용
- 인덱스 필수

---

### 상세 조회 (Detail)

```java
// 전체 엔티티 조회 OK
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findById(@Param("id") Long id);

// 또는 Fetch Join
@Query("SELECT p FROM Product p " +
        "LEFT JOIN FETCH p.options " +
        "WHERE p.id = :id")
Optional<Product> findByIdWithOptions(@Param("id") Long id);
```

**포인트**:
- 엔티티 전체 조회 허용
- 연관 엔티티 필요 시 Fetch Join

---

### 집계 쿼리 (Aggregation)

```java
// GROUP BY, SUM, COUNT 활용
@Query(value = "SELECT oi.product_option_id, " +
        "SUM(oi.quantity) as total_quantity " +
        "FROM order_items oi " +
        "WHERE oi.created_at >= :startDate " +
        "GROUP BY oi.product_option_id " +
        "ORDER BY total_quantity DESC " +
        "LIMIT 5",
        nativeQuery = true)
List<Object[]> findTopProducts(@Param("startDate") LocalDateTime startDate);
```

**포인트**:
- Native Query 사용 (집계 함수)
- DB에서 계산 (애플리케이션 X)
- 인덱스 활용

---

### 업데이트 쿼리

```java
// Bulk Update (대량 업데이트)
@Modifying
@Query("UPDATE UserCoupon uc SET uc.status = :status " +
        "WHERE uc.id IN :ids")
void updateStatusByIds(
    @Param("status") UserCouponStatus status,
    @Param("ids") List<Long> ids
);
```

**주의**:
- `@Modifying` 필수
- 영속성 컨텍스트 갱신 안 됨 (재조회 필요)

---

## 🔍 JPQL vs Native Query

### JPQL 사용

```java
@Query("SELECT p FROM Product p WHERE p.status = :status")
List<Product> findByStatus(@Param("status") ProductStatus status);
```

**장점**:
- 엔티티 반환
- 타입 안전
- DB 독립적

**언제**:
- 단순 조회
- 엔티티 전체 필요
- Fetch Join

---

### Native Query 사용

```java
@Query(value = "SELECT p.id, p.name, " +
        "COALESCE(SUM(po.stock), 0) as total_stock " +
        "FROM products p " +
        "LEFT JOIN product_options po ON p.id = po.product_id " +
        "GROUP BY p.id, p.name",
        nativeQuery = true)
List<Object[]> findWithTotalStock();
```

**장점**:
- 복잡한 쿼리 가능
- 집계 함수 (SUM, COUNT)
- DB 특화 기능

**언제**:
- 집계 쿼리
- 복잡한 JOIN
- DTO Projection
- 성능 최적화

---

## 📊 성능 비교

### SELECT * vs 필요한 컬럼만

| 쿼리 | 조회 데이터 | 실행 시간 |
|------|-----------|----------|
| SELECT * | 10MB | 250ms |
| SELECT id, name | 500KB | 15ms |

**개선율**: 16배

---

### 메모리 필터링 vs DB 필터링

| 방법 | 조회 레코드 | 실행 시간 | 메모리 |
|------|-----------|----------|--------|
| findAll() + filter | 1,000,000 | 3,500ms | 500MB |
| WHERE 절 | 2,740 | 15ms | 1.5MB |

**개선율**: 233배

---

### N+1 vs JOIN

| 방법 | 쿼리 수 | 실행 시간 |
|------|--------|----------|
| 반복 조회 | 5,001 | 12,500ms |
| JOIN | 1 | 25ms |

**개선율**: 500배

---

## ⚠️ 주의사항

### 1. 페이징 시 Fetch Join 주의

```java
// ❌ 경고 발생: firstResult/maxResults specified with collection fetch
@Query("SELECT p FROM Product p " +
        "JOIN FETCH p.options " +
        "ORDER BY p.createdAt DESC")
Page<Product> findAllWithOptions(Pageable pageable);
```

**해결**: Batch Size 사용
```java
@BatchSize(size = 10)
@OneToMany(mappedBy = "product")
private List<ProductOption> options;
```

---

### 2. IN 절 크기 제한

```java
// ❌ IN 절에 1000개 이상 주의 (Oracle 1000개 제한)
@Query("SELECT p FROM Product p WHERE p.id IN :ids")
List<Product> findByIdIn(@Param("ids") List<Long> ids);
```

**해결**: 1000개씩 분할 처리

---

### 3. 동적 쿼리

```java
// ❌ 동적 쿼리 어려움
@Query("SELECT p FROM Product p WHERE ...")
```

**해결**: QueryDSL 또는 Specification 사용

---

## 📋 체크리스트

쿼리 작성 시 확인:

- [ ] SELECT * 사용하지 않았는가?
- [ ] WHERE 절로 DB에서 필터링하는가?
- [ ] N+1 문제 없는가?
- [ ] JOIN 필요한 곳에 사용했는가?
- [ ] 인덱스가 있는가?
- [ ] 메모리에 전체 데이터 로드하지 않는가?
- [ ] COUNT 대신 EXISTS 사용 가능한가?
- [ ] Native Query가 필요한가? (집계, 복잡한 JOIN)

---

## 🎯 결론

### 핵심 원칙
1. **필요한 것만 조회** (SELECT *)
2. **DB에서 필터링** (WHERE)
3. **JOIN 활용** (N+1 방지)
4. **인덱스 필수** (성능)

### 성능 개선 효과
- 쿼리 수: 평균 99% 감소
- 실행 시간: 평균 200배 개선
- 메모리: 평균 99% 감소

### 권장 사항
- 목록 조회: Native Query + DTO Projection
- 상세 조회: JPQL + Fetch Join
- 집계 쿼리: Native Query
- 업데이트: Bulk Update
