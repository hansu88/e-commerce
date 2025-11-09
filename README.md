# 이커머스 프로젝트

> 항해플러스 백엔드 코스 - STEP 5-6

---

## 📌 프로젝트 개요

선착순 쿠폰 발급, 재고 관리, 주문/결제 기능을 갖춘 이커머스 플랫폼 (인메모리 기반)

**핵심 기능:**
- 🎟️ 선착순 쿠폰 발급 (동시성 제어)
- 📦 재고 관리 (동시성 제어)
- 🛒 주문 및 결제
- 📊 인기 상품 집계

---

## 🛠️ 기술 스택

- **Language:** Java 17
- **Framework:** Spring Boot 3.4.11
- **Build Tool:** Gradle 8.14.3
- **Testing:** JUnit 5, AssertJ
- **Storage:** InMemory (ConcurrentHashMap)
- **Documentation:** Swagger/OpenAPI 3.0

---

## 🏗️ 아키텍처

### UseCase 패턴 (Clean Architecture)

```
Presentation (Controller)
    ↓
Application (UseCase + Command)  ← 단일 책임 원칙
    ↓
Domain (Entity)
    ↓
Infrastructure (Repository)
```

### 디렉토리 구조

```
application/
├── command/          # 명령 객체 (15개)
│   ├── CreateOrderCommand
│   ├── IssueCouponCommand
│   └── DecreaseStockCommand
│
└── usecase/          # 비즈니스 로직 (18개)
    ├── order/
    │   ├── CreateOrderUseCase
    │   ├── PayOrderUseCase
    │   └── CancelOrderUseCase
    ├── coupon/
    │   ├── IssueCouponUseCase
    │   ├── UseCouponUseCase
    │   └── RestoreCouponUseCase
    ├── stock/
    │   ├── DecreaseStockUseCase
    │   ├── IncreaseStockUseCase
    │   └── ValidateStockUseCase
    ├── product/
    │   ├── GetProductListUseCase
    │   ├── GetProductDetailUseCase
    │   └── GetPopularProductsUseCase
    └── cart/
        ├── AddCartItemUseCase
        ├── GetCartItemsUseCase
        └── DeleteCartItemUseCase
```

### 실행 흐름 예시

```java
// Controller
@PostMapping
public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
    CreateOrderCommand command = new CreateOrderCommand(userId, items, couponId);
    Order order = createOrderUseCase.execute(command);
    return ResponseEntity.ok(order);
}

// UseCase
@Component
public class CreateOrderUseCase {
    public Order execute(CreateOrderCommand command) {
        // 1. 재고 차감
        decreaseStockUseCase.execute(...);

        // 2. 쿠폰 적용
        useCouponUseCase.execute(...);

        // 3. 주문 생성
        return orderRepository.save(order);
    }
}
```

---

## 📡 API 명세

상세 명세: [api-specification.md](docs/api/api-specification.md)

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

| 카테고리 | 엔드포인트 | 메서드 | 설명 |
|---------|----------|--------|------|
| 상품 | `/api/products` | GET | 전체 상품 조회 |
| 상품 | `/api/products/{id}` | GET | 상품 상세 조회 |
| 상품 | `/api/products/popular` | GET | 인기 상품 조회 |
| 장바구니 | `/api/carts` | POST | 장바구니 담기 |
| 장바구니 | `/api/carts?uid={uid}` | GET | 장바구니 조회 |
| 주문 | `/api/orders` | POST | 주문 생성 |
| 주문 | `/api/orders/{id}/pay` | POST | 결제 처리 |
| 주문 | `/api/orders/{id}` | DELETE | 주문 취소 |
| 쿠폰 | `/api/coupons/{id}/issue` | POST | 쿠폰 발급 |
| 쿠폰 | `/api/coupons/my?uid={uid}` | GET | 내 쿠폰 조회 |

---

## 🔐 동시성 제어

### 구현 방식

`synchronized` + `ConcurrentHashMap` 기반 **ID별 Lock**

```java
@Component
public class IssueCouponUseCase {
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public UserCoupon execute(IssueCouponCommand command) {
        // Coupon ID별 Lock 획득
        Object lock = lockMap.computeIfAbsent(command.getCouponId(), k -> new Object());

        synchronized (lock) {
            // Check-Then-Act를 원자적으로 처리
            if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
                throw new IllegalStateException("쿠폰 발급 한도 초과");
            }
            coupon.setIssuedQuantity(coupon.getIssuedQuantity() + 1);
            return userCouponRepository.save(userCoupon);
        }
    }
}
```

### 검증 결과

| 테스트 시나리오 | 결과 |
|---------------|------|
| 쿠폰 100개, 요청 100개 | ✅ 100개 발급, 0개 실패 |
| 쿠폰 100개, 요청 200개 | ✅ 100개 발급, 100개 실패 |
| 재고 100개, 요청 150개 | ✅ 100개 성공, 50개 실패 |
| 재고 10개, 요청 20개 | ✅ 재고 음수 방지 |

상세 분석: [동시성_제어_분석_보고서.md](docs/동시성_제어_분석_보고서.md)

---

## 🧪 테스트

### 실행 방법

```bash
# 전체 테스트
./gradlew test

# 동시성 테스트만
./gradlew test --tests "*ConcurrencyTest"

# UseCase 테스트만
./gradlew test --tests "*.usecase.*"

# 테스트 커버리지 리포트
./gradlew test jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

### 테스트 현황

| 항목 | 개수 | 상태 |
|------|------|------|
| 테스트 커버리지 | 76% | ✅ |
| UseCase 단위 테스트 | 4개 | ✅ |
| 동시성 테스트 | 7개 시나리오 | ✅ |
| Controller 테스트 | 4개 | ✅ |
| 통합 테스트 | 1개 | ✅ |

---

## 🚀 실행

```bash
# Java 17 설정 (필수)
export JAVA_HOME="/path/to/jdk-17"

# 빌드
./gradlew clean build

# 실행
./gradlew bootRun

# 애플리케이션 접속
# http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 📊 구현 완료

### STEP 5: 레이어드 아키텍처
- [x] 도메인 모델 (7개 도메인)
- [x] UseCase 패턴 적용 (18개 UseCase)
- [x] Command 객체 (15개)
- [x] 책임 분리 (단일 책임 원칙)
- [x] Repository 패턴 (18개 Repository)

### STEP 6: 동시성 제어
- [x] Race Condition 방지 (ID별 Lock)
- [x] 동시성 테스트 (7개 시나리오, 100% 통과)
- [x] 쿠폰 선착순 발급 (정확히 한도만큼만)
- [x] 재고 Over-selling 방지
- [x] 인기 상품 집계
- [x] 문서화 (README + 분석 보고서)

### 추가 구현
- [x] Swagger/OpenAPI 문서화
- [x] @Deprecated 처리 (기존 Service)
- [x] 테스트 커버리지 76%
