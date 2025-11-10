# 데이터 모델 / ERD (최신)

## 설계 철학
- **FK 제약조건 없음**: 애플리케이션 레벨에서 참조 무결성 관리 (MSA 대비, 샤딩 용이)
- **낙관적 락**: version 컬럼으로 동시성 제어 (Coupon, ProductOption)
- **감사(Audit) 필드**: 모든 테이블에 createdAt, updatedAt 추가
- **인덱스 전략**: 복합 인덱스는 선택도 높은 컬럼 우선

---

## 엔티티 목록

### 1. Product (상품)
**테이블명**: `products`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 상품 ID |
| name | VARCHAR(100) | NOT NULL | 상품명 |
| price | INTEGER | NOT NULL | 가격 |
| status | ENUM | NOT NULL | ACTIVE, INACTIVE, SOLD_OUT |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_status_created` (status, created_at)
- `idx_name` (name)

**관계**: ~~1:N ProductOption~~ (컬렉션 제거, ID 참조만 사용)

---

### 2. ProductOption (상품 옵션)
**테이블명**: `product_options`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 옵션 ID |
| product_id | BIGINT | NOT NULL | 상품 ID (FK 없음) |
| color | VARCHAR(50) | NOT NULL | 색상 |
| size | VARCHAR(20) | NOT NULL | 사이즈 |
| stock | INTEGER | NOT NULL | 재고 수량 |
| version | BIGINT | | 낙관적 락 (재고 동시성 제어) |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_product_stock` (product_id, stock)
- `idx_product_id` (product_id)

**관계**: ~~1:N CartItem, 1:N OrderItem, 1:N StockHistory~~ (컬렉션 제거)

---

### 3. Cart (장바구니)
**테이블명**: `carts`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 장바구니 ID |
| user_id | BIGINT | NOT NULL, UNIQUE | 사용자 ID (1인 1장바구니) |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_user_id` (user_id) - UNIQUE

**관계**: ~~1:N CartItem~~ (컬렉션 제거)

---

### 4. CartItem (장바구니 아이템)
**테이블명**: `cart_items`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 아이템 ID |
| cart_id | BIGINT | NOT NULL | 장바구니 ID (FK 없음) |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (FK 없음) |
| quantity | INTEGER | NOT NULL | 수량 |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_cart_id` (cart_id)

---

### 5. Order (주문)
**테이블명**: `orders`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| user_id | BIGINT | NOT NULL | 사용자 ID (FK 없음) |
| status | ENUM | NOT NULL | CREATED, PAID, CANCELLED |
| total_amount | INTEGER | NOT NULL | 최종 결제 금액 (할인 적용 후) |
| discount_amount | INTEGER | | 할인 금액 (쿠폰) |
| user_coupon_id | BIGINT | | 사용한 쿠폰 ID (취소 시 복구용) |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_user_created` (user_id, created_at)
- `idx_status_created` (status, created_at)

**관계**: ~~1:N OrderItem~~ (컬렉션 제거)

---

### 6. OrderItem (주문 아이템)
**테이블명**: `order_items`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 아이템 ID |
| order_id | BIGINT | NOT NULL | 주문 ID (FK 없음) |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (FK 없음) |
| quantity | INTEGER | NOT NULL | 수량 |
| price | INTEGER | NOT NULL | 단가 (주문 시점 가격) |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_order_id` (order_id)
- `idx_option_created` (product_option_id, created_at) - 인기 상품 집계용

---

### 7. Coupon (쿠폰 마스터)
**테이블명**: `coupons`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 쿠폰 ID |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 쿠폰 코드 |
| discount_amount | INTEGER | NOT NULL | 할인 금액 |
| total_quantity | INTEGER | NOT NULL | 총 발급 가능 수량 |
| issued_quantity | INTEGER | NOT NULL | 현재까지 발급된 수량 |
| version | BIGINT | | 낙관적 락 (동시 발급 제어) |
| valid_from | DATETIME(6) | NOT NULL | 유효 시작일 |
| valid_until | DATETIME(6) | NOT NULL | 유효 종료일 |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_code` (code) - UNIQUE

**관계**: ~~1:N UserCoupon~~ (컬렉션 제거)

---

### 8. UserCoupon (발급된 쿠폰)
**테이블명**: `user_coupons`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 발급 ID |
| user_id | BIGINT | NOT NULL | 사용자 ID (FK 없음) |
| coupon_id | BIGINT | NOT NULL | 쿠폰 ID (FK 없음) |
| issued_at | DATETIME(6) | NOT NULL | 발급 일시 |
| used_at | DATETIME(6) | | 사용 일시 |
| status | ENUM | NOT NULL | AVAILABLE, USED, EXPIRED |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_user_status` (user_id, status)

---

### 9. StockHistory (재고 이력)
**테이블명**: `stock_histories`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (FK 없음) |
| change_qty | INTEGER | NOT NULL | 변동 수량 (양수: 증가, 음수: 감소) |
| reason | ENUM | NOT NULL | ORDER, CANCEL, RESTOCK |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |
| updated_at | DATETIME(6) | | 수정일시 |

**인덱스**:
- `idx_option_created` (product_option_id, created_at)

---

## 관계 정리

**참고**: FK 제약조건 없이 애플리케이션 레벨에서만 관계 관리

```
Product (1) -----> (N) ProductOption [product_id]
ProductOption (1) -----> (N) CartItem [product_option_id]
ProductOption (1) -----> (N) OrderItem [product_option_id]
ProductOption (1) -----> (N) StockHistory [product_option_id]

Cart (1) -----> (N) CartItem [cart_id]
Order (1) -----> (N) OrderItem [order_id]
Coupon (1) -----> (N) UserCoupon [coupon_id]

Order -----> UserCoupon [user_coupon_id] (Optional, 쿠폰 사용 시)
```

---

## 동시성 제어 전략

### 낙관적 락 (@Version)
- **ProductOption.stock**: 재고 차감 시 동시성 제어
- **Coupon.issuedQuantity**: 쿠폰 발급 시 동시성 제어

### 비관적 락 (미래 고려사항)
- 필요 시 `@Lock(PESSIMISTIC_WRITE)` 적용 가능

---

## 인덱스 전략 요약

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| products | (status, created_at) | 상태별 최신 상품 조회 |
| products | (name) | 상품명 검색 |
| product_options | (product_id, stock) | 재고 있는 옵션 조회 |
| carts | (user_id) UNIQUE | 사용자별 장바구니 조회 |
| orders | (user_id, created_at) | 사용자 주문 이력 조회 |
| orders | (status, created_at) | 상태별 주문 관리 |
| order_items | (product_option_id, created_at) | 인기 상품 집계 |
| coupons | (code) UNIQUE | 쿠폰 코드 조회 |
| user_coupons | (user_id, status) | 사용자 사용 가능 쿠폰 조회 |
| stock_histories | (product_option_id, created_at) | 재고 이력 추적 |

---

## 미래 확장 고려사항

### 샤딩 준비
- FK 없음 → 테이블 분산 가능
- user_id 기준 샤딩 가능 (carts, orders, user_coupons)

### 파티셔닝
- stock_histories: created_at 기준 월별 파티셔닝
- order_items: created_at 기준 월별 파티셔닝

### 읽기 전용 집계 테이블
- popular_products: 인기 상품 순위 (배치 집계)
- 일별 판매량, 재고 현황 등 별도 테이블 구성 가능
