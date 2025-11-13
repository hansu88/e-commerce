## :pushpin: [STEP07,STEP08 김한수 - 실제 DB 기반 JPARepository 관련 작업]

### ✅ **STEP07: DB 설계 개선 및 구현**

- [x] **기존 설계된 테이블 구조에 대한 개선점이 반영되었는가?**
  - PopularProduct 집계 테이블 추가 (일별/월별 판매량 집계)
  - `order_items.idx_created_at` 인덱스 추가 (집계 쿼리 최적화)
  - `user_coupons.uk_user_coupon` UNIQUE 제약조건 추가 (중복 발급 방지)
  - 낙관적 락(@Version) 추가: Coupon, ProductOption

- [x] **Repository 및 데이터 접근 계층이 역할에 맞게 분리되어 있는가?**
  - `infrastructure.persistence.base` 패키지에 JpaRepository 계층 분리
  - Repository 인터페이스와 구현체 분리 완료

- [x] **MySQL 기반으로 연동되고 동작하는가?**
  - MySQL 8.0 연동 완료 (192.168.4.81:3306)
  - `application.yml`에 MySQL 설정 완료
  - 테스트 실행 시 정상 동작 확인

- [x] **infrastructure 레이어를 포함하는 통합 테스트가 작성되었는가?**
  - `ECommerceIntegrationTest` 작성 완료 (3개 시나리오)
    1. 상품 조회 -> 장바구니 -> 주문 생성 (쿠폰 없이)
    2. 쿠폰 발급 -> 장바구니 -> 주문 생성 (쿠폰 사용)
    3. 여러 옵션 장바구니 담기 후 주문
  - Repository, UseCase, 전체 플로우 검증

- [x] **핵심 기능에 대한 흐름이 테스트에서 검증되었는가?**
  - UseCase 단위 테스트로 핵심 비즈니스 로직 검증
  - 통합 테스트로 전체 플로우 검증
  - 주요 기능: 상품 조회, 장바구니, 쿠폰 발급, 주문 생성, 재고 관리

- [x] **기존에 작성된 동시성 테스트가 잘 통과하는가?**
  - **CouponConcurrencyTest (3개 시나리오)**: ✅ 모두 통과
    - 쿠폰 100개, 100명 동시 요청 → 100명 모두 발급
    - 쿠폰 100개, 200명 동시 요청 → 100명만 발급
    - 쿠폰 50개, 100명 동시 요청 → 50명만 발급
  - **StockConcurrencyTest (2개 시나리오)**: ✅ 모두 통과
    - 재고 100개, 100명 동시 차감 → 재고 0개
    - 재고 50개, 100명 동시 차감 → 50명만 성공

---

### 🔥 **STEP08: 쿼리 및 인덱스 최적화**

- [x] **조회 성능 저하 가능성이 있는 기능을 식별하였는가?**

**1. GetProductListUseCase - N+1 쿼리 문제**
```java
// BEFORE: 1 + N개 쿼리 (N = 상품 개수)
List<Product> products = productRepository.findAll();
for (Product p : products) {
    // 각 상품마다 옵션 조회 → N+1 문제
    int totalStock = productOptionRepository.findByProductId(p.getId())
        .stream().mapToInt(ProductOption::getStock).sum();
}
```

**2. AggregatePopularProductsUseCase - 메모리 과부하**
```java
// BEFORE: 전체 OrderItem을 메모리에 로드 후 필터링
List<OrderItem> allItems = orderItemRepository.findAll(); // 위험!
List<OrderItem> filtered = allItems.stream()
    .filter(item -> item.getCreatedAt() >= start && item.getCreatedAt() < end)
    .toList();
```

**3. ExpireUserCouponsUseCase - N+1 쿼리 문제**
```java
// BEFORE: UserCoupon 조회 후 각각 Coupon 조회
List<UserCoupon> userCoupons = userCouponRepository.findAll();
for (UserCoupon uc : userCoupons) {
    Coupon coupon = couponRepository.findById(uc.getCouponId()); // N+1
    if (coupon.getValidUntil().isBefore(now)) { ... }
}
```

---

- [x] **쿼리 실행계획(Explain) 기반으로 문제를 분석하였는가?**

**분석 결과:**
1. **GetProductListUseCase**: 101개 쿼리 실행 (상품 100개 기준)
   - 1회: SELECT * FROM products
   - 100회: SELECT * FROM product_options WHERE product_id = ?

2. **AggregatePopularProductsUseCase**: Full Table Scan 발생
   - created_at 컬럼에 인덱스 없음 → WHERE 절 필터링 시 Full Scan
   - 메모리 필터링 → 대량 데이터 시 OutOfMemoryError 위험

3. **ExpireUserCouponsUseCase**: 1 + N개 쿼리
   - UserCoupon 조회 후 각 Coupon을 개별 조회

---

- [x] **인덱스 설계 또는 쿼리 구조 개선 등 해결방안을 도출하였는가?**

## 🚀 **적용한 최적화**

### 1️⃣ GetProductListUseCase - Native Query + LEFT JOIN

**AS-IS (N+1 문제):**
```java
return productRepository.findAll().stream()
    .map(product -> {
        int totalStock = productOptionRepository.findByProductId(product.getId())
            .stream().mapToInt(ProductOption::getStock).sum();
        return new ProductListResponseDto(..., totalStock);
    });
```

**TO-BE (단일 쿼리):**
```java
@Query(value = "SELECT p.id, p.name, p.price, p.status, " +
        "COALESCE(SUM(po.stock), 0) as total_stock " +
        "FROM products p " +
        "LEFT JOIN product_options po ON p.id = po.product_id " +
        "GROUP BY p.id, p.name, p.price, p.status " +
        "ORDER BY p.id",
        nativeQuery = true)
List<Object[]> findAllWithTotalStockNative();
```

**성능 개선:** ~100배 향상 (101개 쿼리 → 1개 쿼리)

---

### 2️⃣ AggregatePopularProductsUseCase - DB 필터링 + 인덱스

**AS-IS (메모리 필터링):**
```java
List<OrderItem> orderItems = orderItemRepository.findAll().stream()
    .filter(item -> item.getCreatedAt() >= start && item.getCreatedAt() < end)
    .toList();
```

**TO-BE (DB 필터링 + 인덱스):**
```java
// Repository 메서드 추가
@Query("SELECT oi FROM OrderItem oi " +
        "WHERE oi.createdAt >= :start AND oi.createdAt < :end")
List<OrderItem> findByCreatedAtBetween(
    @Param("start") LocalDateTime start,
    @Param("end") LocalDateTime end);

// Entity에 인덱스 추가
@Table(name = "order_items",
    indexes = {
        @Index(name = "idx_created_at", columnList = "created_at")  // 추가
    })
```

**성능 개선:**
- 메모리 사용량 대폭 감소
- 인덱스 스캔으로 Full Table Scan 방지

---

### 3️⃣ ExpireUserCouponsUseCase - JOIN 쿼리

**AS-IS (N+1 문제):**
```java
List<UserCoupon> userCoupons = userCouponRepository.findByStatus(AVAILABLE);
for (UserCoupon uc : userCoupons) {
    Coupon coupon = couponRepository.findById(uc.getCouponId()).orElse(null);
    if (coupon != null && coupon.getValidUntil().isBefore(now)) {
        uc.expire();
    }
}
```

**TO-BE (단일 JOIN 쿼리):**
```java
@Query(value = "SELECT uc.* FROM user_coupons uc " +
        "INNER JOIN coupons c ON uc.coupon_id = c.id " +
        "WHERE uc.status = 'AVAILABLE' AND c.valid_until < :now",
        nativeQuery = true)
List<UserCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);
```

**성능 개선:** N+1 쿼리 제거

---

### 4️⃣ 중복 발급 방지 - UNIQUE 제약조건

**AS-IS (애플리케이션 레벨 체크):**
```java
if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
    throw new IllegalStateException("이미 발급받은 쿠폰");
}
// Race Condition 가능 ⚠️
```

**TO-BE (DB 레벨 제약조건):**
```java
@Table(name = "user_coupons",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_coupon",
            columnNames = {"user_id", "coupon_id"})
    })
```

**장점:**
- Race Condition 완전 방지
- 데이터 무결성 보장
- 애플리케이션 코드 간소화

---

### 5️⃣ 동시성 제어 - 낙관적 락 재시도 최적화

**쿠폰 발급 동시성 문제:**
- 100명이 동시에 쿠폰 100개 발급 시 낙관적 락 경합 발생

**해결 방법:**
```java
// AS-IS
private static final int MAX_RETRIES = 30;
Thread.sleep(retryCount * 5L); // 최대 150ms 대기

// TO-BE
private static final int MAX_RETRIES = 100; // 재시도 횟수 증가
Thread.sleep(retryCount * 2L);  // 백오프 시간 최적화 (최대 200ms)
```

**추가 최적화 검토 사항:**
```
낙관적 락 경합을 줄이는 추가 방법:
1. Redis 분산 락 사용 (Redisson)
2. 메시지 큐 사용 (RabbitMQ, Kafka)
3. DB 배치 처리 (재고 예약 시스템)
4. 비관적 락 (FOR UPDATE) - 단, 성능 저하 가능
```

---

## 📊 **테스트 및 품질**

| 항목 | 결과 |
|------|------|
| **테스트 커버리지** | **62%** (JaCoCo) |
| **전체 테스트 수** | **45개** (모두 통과 ✅) |
| **단위 테스트** | 39개 (UseCase 11개, Controller 3개) |
| **통합 테스트** | 3개 (전체 플로우 검증) |
| **동시성 테스트** | **5개 (모두 통과 ✅)** |
| └ 쿠폰 동시성 | 3개 시나리오 (100명, 200명, 100명) |
| └ 재고 동시성 | 2개 시나리오 (100명, 100명) |

**주요 패키지별 커버리지:**
- `application.usecase.product`: **98%** ⭐
- `application.usecase.stock`: **81%**
- `application.usecase.coupon`: **67%**
- `presentation.controller`: **66%**
- `application.usecase.order`: **50%**

---

## 💬 **리뷰 요청 사항**

### 질문/고민 포인트

1. **Service vs UseCase 아키텍처**
   - 현재 모든 비즈니스 로직을 UseCase로 구현했습니다
   - 거대해질수록 Service도 필요하다는 의견을 들었는데, 어떻게 분리하는 게 좋을까요?

2. **Command 패턴의 필요성**
   - 현재 Command에 유효성 검증을 넣었습니다
   - 실무에서 Command 패턴이 실제로 유용한지, 과한 추상화는 아닌지 궁금합니다

3. **결제 로직 분리**
   - 현재 CreateOrderUseCase에 재고차감/결제를 모두 포함했습니다
   - 결제는 다양한 방식(카드/계좌/간편결제)과 실패 처리가 복잡해서 별도 분리를 고려 중입니다

4. **아키텍처 방향성**
   - 현재 Layered Architecture + DDD 혼합 형태입니다
   - 실무에서도 이렇게 섞여서 사용하는지, 아니면 명확히 구분하는지 궁금합니다

5. **실시간 vs 배치 처리**
   - 인기 상품 집계: 현재 스케줄러로 배치 처리 (일별/월별)
   - 쿠폰 만료: 매시간 스케줄러로 처리
   - 재고 임계치 알림: 실시간 vs 배치 고민 중
   - 이커머스에서 Spring Batch를 실제로 사용하는지, 아니면 실시간 처리가 우선인지 궁금합니다

---

## 📝 **주요 변경 사항**

### 엔티티 추가/수정
- ✅ PopularProduct 집계 테이블 추가
- ✅ OrderItem, UserCoupon 인덱스 추가
- ✅ Coupon, ProductOption 낙관적 락 추가

### 성능 최적화
- ✅ GetProductListUseCase N+1 해결 (Native Query)
- ✅ AggregatePopularProductsUseCase DB 필터링
- ✅ ExpireUserCouponsUseCase JOIN 쿼리
- ✅ 쿠폰 중복 발급 UNIQUE 제약조건

### 테스트
- ✅ 통합 테스트 3개 추가 (ECommerceIntegrationTest)
- ✅ 동시성 테스트 모두 통과 (재시도 로직 최적화)
- ✅ 테스트 커버리지 62% 달성

### 문서화
- ✅ data-models.md 작성 (10개 Entity 문서화)
