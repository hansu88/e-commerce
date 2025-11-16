# 데이터 모델 / ERD (최신)

## 설계 철학
- **FK 제약조건 없음**: 애플리케이션 레벨에서 참조 무결성 관리 (MSA 대비, 샤딩 용이)
- **낙관적 락**: version 컬럼으로 동시성 제어 (Coupon, ProductOption)
- **감사(Audit) 필드**: 모든 테이블에 createdAt, updatedAt 추가
- **인덱스 전략**: 복합 인덱스는 선택도 높은 컬럼 우선
- **UNIQUE 제약조건**: DB 레벨에서 데이터 무결성 보장 (UserCoupon, PopularProduct)

---

## 엔티티 목록

## 1. Product (상품)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 상품 ID |
| name | VARCHAR(100) | NOT NULL | 상품명 |
| price | INT | NOT NULL | 가격 |
| status | VARCHAR(20) | NOT NULL | ProductStatus (ACTIVE/INACTIVE/SOLD_OUT) |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**Enum: ProductStatus**
- `ACTIVE`: 판매 중인 상품
- `INACTIVE`: 판매 중지 상품
- `SOLD_OUT`: 품절 상품

---

## 2. ProductOption (상품 옵션)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 옵션 ID |
| product_id | BIGINT | NOT NULL | 상품 ID (논리적 FK) |
| color | VARCHAR(50) | NOT NULL | 색상 |
| size | VARCHAR(20) | NOT NULL | 사이즈 |
| stock | INT | NOT NULL | 재고 |
| version | BIGINT | NULL | 낙관적 락 버전 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**인덱스:**
- `idx_product_id`: (product_id) - 상품별 옵션 조회 최적화

**동시성 제어:**
- @Version 필드로 낙관적 락 적용 (재고 차감 시)

---

## 3. PopularProduct (인기 상품 집계) **[신규 추가]**
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 집계 ID |
| product_id | BIGINT | NOT NULL | 상품 ID (논리적 FK) |
| period_type | VARCHAR(20) | NOT NULL | PeriodType (DAILY/MONTHLY) |
| sales_count | INT | NOT NULL | 판매 수량 |
| aggregated_date | DATE | NOT NULL | 집계 기준일 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**Enum: PeriodType**
- `DAILY`: 일별 집계
- `MONTHLY`: 월별 집계

**인덱스:**
- `idx_period_sales`: (period_type, sales_count DESC) - 인기 상품 조회 최적화
- `idx_aggregated_date`: (aggregated_date) - 기간별 조회 최적화

**제약조건:**
- `uk_product_period_date`: UNIQUE(product_id, period_type, aggregated_date) - 중복 집계 방지

**비즈니스 로직:**
- 매일 02:00 AM - 전일 일별 집계
- 매월 1일 03:00 AM - 전월 월별 집계

---

## 4. StockHistory (재고 이력)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (논리적 FK) |
| change_qty | INT | NOT NULL | 재고 변화량 (+ 증가, - 감소) |
| reason | VARCHAR(20) | NOT NULL | StockChangeReason (ORDER/CANCEL/RESTOCK) |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**Enum: StockChangeReason**
- `ORDER`: 주문으로 인한 재고 차감
- `CANCEL`: 주문 취소로 인한 재고 증가
- `RESTOCK`: 재입고

---

## 5. Coupon (쿠폰)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 쿠폰 ID |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 고유 쿠폰 코드 |
| discount_amount | INT | NOT NULL | 할인 금액 |
| total_quantity | INT | NOT NULL | 총 발급 가능 수량 |
| issued_quantity | INT | NOT NULL | 현재 발급된 수량 |
| version | BIGINT | NULL | 낙관적 락 버전 |
| valid_from | DATETIME | NOT NULL | 유효 시작일 |
| valid_until | DATETIME | NOT NULL | 유효 종료일 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**인덱스:**
- `idx_code`: (code) UNIQUE - 쿠폰 코드 조회 최적화

**동시성 제어:**
- @Version 필드로 낙관적 락 적용 (발급 수량 증가 시)
- 최대 100회 재시도, 지수 백오프 (2ms씩 증가)

---

## 6. UserCoupon (사용자 쿠폰)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 쿠폰 ID |
| user_id | BIGINT | NOT NULL | 사용자 ID |
| coupon_id | BIGINT | NOT NULL | 쿠폰 ID (논리적 FK) |
| issued_at | DATETIME | NOT NULL | 발급일 |
| used_at | DATETIME | NULL | 사용일 |
| status | VARCHAR(20) | NOT NULL | UserCouponStatus (AVAILABLE/USED/EXPIRED) |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**Enum: UserCouponStatus**
- `AVAILABLE`: 사용 가능 쿠폰
- `USED`: 사용된 쿠폰
- `EXPIRED`: 만료된 쿠폰

**인덱스:**
- `idx_user_status`: (user_id, status) - 사용자별 사용 가능 쿠폰 조회

**제약조건:**
- `uk_user_coupon`: UNIQUE(user_id, coupon_id) - 중복 발급 방지 **[추가]**

**비즈니스 로직:**
- `expire()`: 쿠폰 만료 처리
- `use()`: 쿠폰 사용 처리
- `restore()`: 주문 취소 시 복구
- 매시간 정각 스케줄러로 만료 처리

---

## 7. Order (주문)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| user_id | BIGINT | NOT NULL | 사용자 ID |
| status | VARCHAR(20) | NOT NULL | OrderStatus (CREATED/PAID/CANCELLED) |
| total_amount | INT | NOT NULL | 최종 결제 금액 (할인 적용 후) |
| discount_amount | INT | NULL | 쿠폰 할인액 |
| user_coupon_id | BIGINT | NULL | 사용한 쿠폰 ID (논리적 FK) |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**Enum: OrderStatus**
- `CREATED`: 주문 생성됨
- `PAID`: 결제 완료
- `CANCELLED`: 주문 취소

**인덱스:**
- `idx_user_created`: (user_id, created_at) - 사용자별 주문 이력 조회

---

## 8. OrderItem (주문 항목)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주문 항목 ID |
| order_id | BIGINT | NOT NULL | 주문 ID (논리적 FK) |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (논리적 FK) |
| quantity | INT | NOT NULL | 주문 수량 |
| price | INT | NOT NULL | 주문 당시 가격 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**인덱스:**
- `idx_order_id`: (order_id) - 주문별 항목 조회
- `idx_created_at`: (created_at) - 집계 쿼리 최적화 **[추가]**

**용도:**
- 인기 상품 집계에 활용 (created_at 인덱스 사용)

---

## 9. Cart (장바구니)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 장바구니 ID |
| user_id | BIGINT | NOT NULL, UNIQUE | 사용자 ID |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**인덱스:**
- `idx_user_id`: (user_id) UNIQUE - 사용자별 장바구니 조회

---

## 10. CartItem (장바구니 항목)
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 장바구니 항목 ID |
| cart_id | BIGINT | NOT NULL | 장바구니 ID (논리적 FK) |
| product_option_id | BIGINT | NOT NULL | 상품 옵션 ID (논리적 FK) |
| quantity | INT | NOT NULL | 수량 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NULL | 수정일 |

**인덱스:**
- `idx_cart_id`: (cart_id) - 장바구니별 항목 조회
