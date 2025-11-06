# 이커머스 프로젝트

> 항해플러스 백엔드 코스 - STEP 5-6

---

## 📌 프로젝트 개요

선착순 쿠폰 발급, 재고 관리, 주문/결제 기능을 갖춘 이커머스 플랫폼 (인메모리 기반)

---

## 🛠️ 기술 스택

- Java 17
- Spring Boot 3.4.11
- Gradle 8.14.3
- JUnit 5, AssertJ
- InMemory (ConcurrentHashMap)

---

## 🏗️ 아키텍처

```
Presentation (Controller)
    ↓
Application (Service)
    ↓
Domain (Entity)
    ↓
Infrastructure (Repository)
```

---

## 📡 API 명세

상세 명세: [api-specification.md](docs/api/api-specification.md)

- `GET /api/products` - 상품 목록
- `GET /api/products/{id}` - 상품 상세
- `GET /api/products/popular` - 인기 상품
- `POST /api/carts` - 장바구니 담기
- `POST /api/orders` - 주문 생성
- `POST /api/orders/{id}/pay` - 결제
- `POST /api/coupons/{id}/issue` - 쿠폰 발급

---

## 🔐 동시성 제어

`synchronized` + `ConcurrentHashMap` 사용

```java
private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

synchronized (lockMap.computeIfAbsent(id, k -> new Object())) {
    // 원자적 처리
}
```

상세 분석: [동시성_제어_분석_보고서.md](docs/동시성_제어_분석_보고서.md)

---

## 🧪 테스트

```bash
# 전체 테스트
./gradlew test

# 동시성 테스트
./gradlew test --tests "*ConcurrencyTest"

# 테스트 커버리지
./gradlew test jacocoTestReport
```

| 항목 | 결과 |
|------|------|
| 테스트 커버리지 | 76% |
| 단위 테스트 | 4개 |
| 통합 테스트 | 1개 |
| 동시성 테스트 | 7개 |
| Controller 테스트 | 4개 |

---

## 🚀 실행

```bash
./gradlew build
./gradlew bootRun
```

---

## 📊 구현 완료

### STEP 5: 레이어드 아키텍처
- [x] 도메인 모델
- [x] 유스케이스
- [x] 책임 분리
- [x] Repository 패턴

### STEP 6: 동시성 제어
- [x] Race Condition 방지
- [x] 동시성 테스트 (7개)
- [x] 인기 상품 집계
- [x] 문서화
