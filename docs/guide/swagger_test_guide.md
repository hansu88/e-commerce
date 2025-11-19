# Swagger API 테스트 가이드

이 문서는 Swagger UI (`http://localhost:8080/swagger-ui.html`)에서 각 API를 테스트할 수 있는 구체적인 매개변수를 제공합니다.

---

## 📦 1. Product API (상품 관리)

### 1.1 GET /api/products - 상품 목록 조회

**✅ 성공 케이스:**
- 매개변수 없음
- 응답: 6개 상품 목록

---

### 1.2 GET /api/products/{id} - 상품 상세 조회

**✅ 성공 케이스:**

| ID | 상품명 | 설명 |
|----|--------|------|
| `1` | 기본 티셔츠 | 3개 옵션 (RED/M, BLUE/L, BLACK/XL) |
| `2` | 청바지 | 3개 옵션 (BLUE/28, BLUE/30, BLACK/32) |
| `3` | 후드티 | 3개 옵션 (GRAY/M, NAVY/L, BLACK/XL) |
| `4` | 맨투맨 | 2개 옵션 (WHITE/M, BEIGE/L) |
| `5` | 조거팬츠 | 2개 옵션 (BLACK/M, GRAY/L) |
| `999` | 품절 상품 | 1개 옵션 (RED/M, 재고 0) |

**❌ 실패 케이스:**

| ID | 에러 | 메시지 |
|----|------|--------|
| `9999` | 404 | 상품을 찾을 수 없습니다. |
| `12345` | 404 | 상품을 찾을 수 없습니다. |

---

### 1.3 GET /api/products/popular - 인기 상품 조회

**✅ 성공 케이스:**

| days | limit | 결과 | 1위 상품 |
|------|-------|------|----------|
| `1` | `5` | 최근 1일 인기 상품 | 조거팬츠 (85개) |
| `3` | `5` | 최근 3일 인기 상품 | 기본 티셔츠 (120개) |
| `7` | `5` | 최근 7일 인기 상품 | 청바지 (245개) |
| `30` | `5` | 최근 30일 인기 상품 | 기본 티셔츠 (980개) |
| `3` | `3` | 최근 3일, 상위 3개만 | 기본 티셔츠 (120개) |
| `7` | `2` | 최근 7일, 상위 2개만 | 청바지 (245개) |

**❌ 실패 케이스:**

| days | limit | 에러 | 메시지 |
|------|-------|------|--------|
| `0` | `5` | 400 | 조회 기간은 1~30일 사이여야 합니다. (입력값: 0) |
| `50` | `5` | 400 | 조회 기간은 1~30일 사이여야 합니다. (입력값: 50) |
| `-1` | `5` | 400 | 조회 기간은 1~30일 사이여야 합니다. (입력값: -1) |
| `3` | `0` | 400 | 조회 개수는 1~100 사이여야 합니다. (입력값: 0) |
| `3` | `200` | 400 | 조회 개수는 1~100 사이여야 합니다. (입력값: 200) |

---

## 🛒 2. Cart API (장바구니 관리)

### 2.1 POST /api/carts - 장바구니 담기

**✅ 성공 케이스:**

```json
{
  "userId": 1,
  "productOptionId": 1,
  "quantity": 2
}
```
→ 201 Created, `{"cartItemId": 생성된ID}`

```json
{
  "userId": 2,
  "productOptionId": 4,
  "quantity": 5
}
```
→ 청바지 옵션 5개 추가 성공

```json
{
  "userId": 3,
  "productOptionId": 12,
  "quantity": 10
}
```
→ 조거팬츠 10개 추가 성공

**❌ 실패 케이스:**

| Body | 에러 | 메시지 |
|------|------|--------|
| `{"userId": 1, "productOptionId": 1, "quantity": 0}` | 400 | 수량은 1개 이상이어야 합니다. |
| `{"userId": 1, "productOptionId": 1, "quantity": -5}` | 400 | 수량은 1개 이상이어야 합니다. |
| `{"userId": 1, "productOptionId": 1, "quantity": 150}` | 400 | 한 번에 최대 100개까지만 담을 수 있습니다. |
| `{"userId": 1, "productOptionId": 9999, "quantity": 1}` | 404 | 상품 옵션을 찾을 수 없습니다. |
| `{"userId": 1, "productOptionId": 3, "quantity": 1}` | 400 | 재고가 부족합니다. (옵션 3은 재고 0) |
| `{"userId": 1, "productOptionId": 1, "quantity": 100}` | 400 | 재고가 부족합니다. (요청 100, 재고 50) |

**사용 가능한 상품 옵션 ID:**
- 1: RED/M (재고 50)
- 2: BLUE/L (재고 30)
- 3: BLACK/XL (재고 0) ❌
- 4: BLUE/28 (재고 15)
- 5: BLUE/30 (재고 10)
- 12: BLACK/M (재고 25)
- 13: GRAY/L (재고 20)

---

### 2.2 GET /api/carts?uid={uid} - 장바구니 조회

**✅ 성공 케이스:**

| uid | 결과 |
|-----|------|
| `1` | 초기 Mock 데이터 2개 항목 반환 |
| `2` | 위에서 추가한 항목들 반환 |
| `999` | 빈 배열 `[]` (쿠폰 없는 사용자도 성공) |

**❌ 실패 케이스:**
- 이 API는 기본적으로 실패하지 않음 (빈 장바구니도 200 OK)
- uid를 아예 안 보내면 400 에러 (Spring이 자동 처리)

---

### 2.3 DELETE /api/carts/{id} - 장바구니 항목 삭제

**✅ 성공 케이스:**

| ID | 결과 |
|----|------|
| `1` | 204 No Content (userId 1의 첫 번째 항목 삭제) |
| `2` | 204 No Content (userId 1의 두 번째 항목 삭제) |

**❌ 실패 케이스:**

| ID | 에러 | 메시지 |
|----|------|--------|
| `9999` | 404 | 장바구니 항목을 찾을 수 없습니다. |
| `12345` | 404 | 장바구니 항목을 찾을 수 없습니다. |

---

## 💳 3. Order API (주문/결제 관리)

### 3.1 POST /api/orders - 주문 생성

**✅ 성공 케이스:**

**케이스 1: 쿠폰 없이 주문**
```json
{
  "userId": 1,
  "cartItems": [
    {"cartItemId": 1, "quantity": 2},
    {"cartItemId": 2, "quantity": 1}
  ],
  "couponId": null
}
```
→ 201 Created

**케이스 2: 5천원 쿠폰 적용**
```json
{
  "userId": 2,
  "cartItems": [
    {"cartItemId": 1, "quantity": 1}
  ],
  "couponId": 1
}
```
→ 201 Created, 5천원 할인 적용

**케이스 3: 1만원 쿠폰 적용**
```json
{
  "userId": 3,
  "cartItems": [
    {"cartItemId": 1, "quantity": 3}
  ],
  "couponId": 2
}
```
→ 201 Created, 1만원 할인 적용

**❌ 실패 케이스:**

**1. 장바구니 비어있음:**
```json
{
  "userId": 1,
  "cartItems": [],
  "couponId": null
}
```
→ 400, "장바구니가 비어있습니다."

**2. 수량 0 이하:**
```json
{
  "userId": 1,
  "cartItems": [
    {"cartItemId": 1, "quantity": 0}
  ],
  "couponId": null
}
```
→ 400, "수량은 1개 이상이어야 합니다."

**3. 존재하지 않는 쿠폰:**
```json
{
  "userId": 1,
  "cartItems": [
    {"cartItemId": 1, "quantity": 1}
  ],
  "couponId": 999
}
```
→ 404, "쿠폰을 찾을 수 없습니다."

**4. 이미 사용된 쿠폰 (같은 userId로 같은 쿠폰 2번 사용):**
- 먼저 위 "케이스 2"를 실행
- 다시 같은 요청 실행
→ 409, "이미 사용된 쿠폰입니다."

**사용 가능한 쿠폰 ID:**
- 1: 5천원 할인
- 2: 1만원 할인
- 3: 3천원 할인

---

### 3.2 POST /api/orders/{id} - 주문 결제

**먼저 주문을 생성해야 합니다!** (위 3.1에서 orderId를 받음)

**✅ 성공 케이스:**

**케이스 1: 신용카드 결제**
```
orderId: 1001 (위에서 생성한 ID)
```
```json
{
  "status": "PAID",
  "paymentMethod": "CREDIT_CARD"
}
```
→ 200 OK

**케이스 2: 체크카드 결제**
```
orderId: 1002
```
```json
{
  "status": "PAID",
  "paymentMethod": "DEBIT_CARD"
}
```
→ 200 OK

**케이스 3: 계좌이체 (잔액 충분)**
```
orderId: 1003 (userId 1 또는 2로 생성한 주문)
```
```json
{
  "status": "PAID",
  "paymentMethod": "BANK_TRANSFER"
}
```
→ 200 OK (userId 1: 10만원, userId 2: 5만원)

**❌ 실패 케이스:**

**1. 존재하지 않는 주문:**
```
orderId: 9999
```
```json
{
  "status": "PAID",
  "paymentMethod": "CREDIT_CARD"
}
```
→ 404, "주문을 찾을 수 없습니다."

**2. 이미 결제된 주문 (같은 주문 ID로 2번 결제):**
- 먼저 정상 결제 실행
- 다시 같은 주문 ID로 결제 시도
→ 409, "이미 결제된 주문입니다."

**3. 잘못된 결제 수단:**
```
orderId: 1001
```
```json
{
  "status": "PAID",
  "paymentMethod": "BITCOIN"
}
```
→ 400, "지원하지 않는 결제 수단입니다."

**4. 잔액 부족 (userId 3으로 생성한 주문):**
```
orderId: 1004 (userId 3으로 생성, 잔액 1만원)
```
```json
{
  "status": "PAID",
  "paymentMethod": "BANK_TRANSFER"
}
```
→ 400, "잔액이 부족합니다." (주문 금액이 1만원보다 큼)

**지원 결제 수단:**
- `CREDIT_CARD` (신용카드)
- `DEBIT_CARD` (체크카드)
- `BANK_TRANSFER` (계좌이체, 잔액 체크)
- `MOBILE_PAY` (모바일 결제)

**사용자별 잔액:**
- userId 1: 100,000원
- userId 2: 50,000원
- userId 3: 10,000원 (부족)

---

## 🎟️ 4. Coupon API (쿠폰 관리)

### 4.1 POST /api/coupons/{id}/issue - 쿠폰 발급

**✅ 성공 케이스:**

**케이스 1: 신규가입 쿠폰 (수량 100개)**
```
couponId: 1
```
```json
{
  "userId": 2
}
```
→ 201 Created

**케이스 2: 첫 구매 쿠폰 (수량 50개)**
```
couponId: 2
```
```json
{
  "userId": 3
}
```
→ 201 Created

**케이스 3: VIP 쿠폰 (수량 2개 남음) - 선착순 테스트**
```
couponId: 3
```
```json
{
  "userId": 4
}
```
→ 201 Created (처음 2명만 성공)

**❌ 실패 케이스:**

**1. 존재하지 않는 쿠폰:**
```
couponId: 999
```
```json
{
  "userId": 1
}
```
→ 404, "쿠폰을 찾을 수 없습니다."

**2. 이미 발급받은 쿠폰 (중복 발급):**
- 먼저 쿠폰 1번을 userId 2로 발급
- 다시 같은 쿠폰 발급 시도
```
couponId: 1
```
```json
{
  "userId": 2
}
```
→ 409, "이미 발급받은 쿠폰입니다."

**3. 수량 소진된 쿠폰:**
```
couponId: 4 (블랙프라이데이, 수량 0)
```
```json
{
  "userId": 1
}
```
→ 409, "쿠폰이 모두 소진되었습니다."

**4. 선착순 마감 (쿠폰 3번으로 3번째 시도):**
```
couponId: 3 (VIP, 수량 2개)
```
```json
{
  "userId": 6
}
```
→ 409, "쿠폰이 모두 소진되었습니다." (3번째 사용자부터)

**사용 가능한 쿠폰:**
- 1: 신규가입 5천원 할인 (수량 100)
- 2: 첫 구매 1만원 할인 (수량 50)
- 3: VIP 3천원 할인 (수량 2) ⚠️ 선착순 테스트용
- 4: 블랙프라이데이 2만원 할인 (수량 0) ❌ 소진됨

---

### 4.2 GET /api/coupons/my?uid={uid}&state={state} - 내 쿠폰 조회

**✅ 성공 케이스:**

| uid | state | 결과 |
|-----|-------|------|
| `1` | `ALL` | 전체 쿠폰 3개 (AVAILABLE 1, USED 1, EXPIRED 1) |
| `1` | `AVAILABLE` | 사용 가능한 쿠폰 1개 |
| `1` | `USED` | 사용된 쿠폰 1개 |
| `1` | `EXPIRED` | 만료된 쿠폰 1개 |
| `2` | `ALL` | 위에서 발급받은 쿠폰들 |
| `999` | `ALL` | 빈 배열 `[]` (쿠폰 없는 사용자도 성공) |

**state 생략 시 기본값: `ALL`**
```
/api/coupons/my?uid=1
```
→ 전체 쿠폰 조회

**❌ 실패 케이스:**

| uid | state | 에러 | 메시지 |
|-----|-------|------|--------|
| `1` | `INVALID` | 400 | 잘못된 상태 값입니다. |
| `1` | `PENDING` | 400 | 잘못된 상태 값입니다. |
| `1` | `active` | 400 | 잘못된 상태 값입니다. (대소문자 구분) |

**허용되는 state 값:**
- `AVAILABLE` - 사용 가능
- `USED` - 사용됨
- `EXPIRED` - 만료됨
- `ALL` - 전체

---

## 🎯 추천 테스트 시나리오 (순서대로)

### 시나리오 1: 정상 주문 플로우

```
1. GET /api/products → 상품 목록 확인
2. GET /api/products/1 → 상품 상세 확인 (옵션 ID 확인)
3. POST /api/carts → 장바구니 담기
   {
     "userId": 10,
     "productOptionId": 1,
     "quantity": 2
   }
4. GET /api/carts?uid=10 → 장바구니 확인
5. POST /api/coupons/1/issue → 쿠폰 발급
   {
     "userId": 10
   }
6. GET /api/coupons/my?uid=10&state=AVAILABLE → 쿠폰 확인
7. POST /api/orders → 주문 생성 (쿠폰 적용)
   {
     "userId": 10,
     "cartItems": [{"cartItemId": 생성된ID, "quantity": 2}],
     "couponId": 1
   }
8. POST /api/orders/{orderId} → 결제
   {
     "status": "PAID",
     "paymentMethod": "CREDIT_CARD"
   }
```

---

### 시나리오 2: 실패 케이스 테스트

```
1. GET /api/products/9999 → 404 (존재하지 않는 상품)
2. GET /api/products/popular?days=50 → 400 (범위 초과)
3. POST /api/carts → 400 (재고 부족)
   {
     "userId": 1,
     "productOptionId": 3,
     "quantity": 1
   }
4. POST /api/coupons/4/issue → 409 (수량 소진)
   {
     "userId": 1
   }
5. POST /api/orders/9999 → 404 (존재하지 않는 주문)
   {
     "status": "PAID",
     "paymentMethod": "CREDIT_CARD"
   }
```

---

### 시나리오 3: 선착순 쿠폰 테스트

```
1. POST /api/coupons/3/issue (userId: 20) → ✅ 성공 (1/2)
2. POST /api/coupons/3/issue (userId: 21) → ✅ 성공 (2/2)
3. POST /api/coupons/3/issue (userId: 22) → ❌ 409 (소진)
4. POST /api/coupons/3/issue (userId: 23) → ❌ 409 (소진)
```

---

## 💡 팁

1. **userId 구분**: 각 시나리오마다 다른 userId 사용 (1, 2, 3, 10, 20 등)
2. **순서 중요**: 주문은 장바구니 생성 후, 결제는 주문 생성 후에만 가능
3. **쿠폰 중복**: 같은 userId로 같은 쿠폰은 1번만 발급 가능
4. **선착순 테스트**: 쿠폰 3번으로 동시성 테스트 가능
5. **잔액 테스트**: userId 3으로 주문 생성 후 BANK_TRANSFER 시 실패
