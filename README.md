# 이커머스 프로젝트

**STEP 7-8**

---

## 📌 프로젝트 개요

선착순 쿠폰 발급, 재고 관리, 주문/결제 기능을 갖춘 이커머스 플랫폼

---

## 🛠️ 기술 스택

- **Language:** Java 17
- **Framework:** Spring Boot 3.4.11
- **Database:** MySQL 8.0
- **ORM:** JPA/Hibernate
- **Testing:** JUnit 5, JaCoCo

---

## 🔐 동시성 제어

낙관적 락(@Version) + 재시도 메커니즘

- **Coupon:** 최대 100회 재시도, 점진적 백오프(2ms)
- **ProductOption:** 재고 차감 시 낙관적 락

---

## 🚀 성능 최적화

### N+1 쿼리 해결

- **GetProductListUseCase:** Native Query + LEFT JOIN (101개 → 1개 쿼리)
- **ExpireUserCouponsUseCase:** JOIN 쿼리 (N+1 제거)

### 인덱스 추가

- `order_items.idx_created_at`: 집계 쿼리 최적화
- `popular_products.idx_period_sales`: 인기 상품 조회 최적화

### UNIQUE 제약조건

- `user_coupons.uk_user_coupon`: 중복 발급 방지

---

## 🧪 테스트

```bash
# 전체 테스트
./gradlew test

# 테스트 커버리지
./gradlew test jacocoTestReport