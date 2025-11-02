# API 명세서

## 상품 관리
| 메서드 | URI                                  | 설명       | 요청                 | 응답                                                             | 상태 코드    |
| --- | ------------------------------------ | -------- | ------------------ | -------------------------------------------------------------- | -------- |
| GET | /api/products                        | 상품 목록 조회 | -                  | [{id, name, price, status, stock}]                             | 200      |
| GET | /api/products/{id}                   | 상품 상세 조회 | path: id           | {id, name, price, status, options: [{id, color, size, stock}]} | 200, 404 |
| GET | /api/products/popular?days=3&limit=5 | 인기 상품 조회 | query: days, limit | [{id, name, soldCount}]                                        | 200      |

## 장바구니
| 메서드    | URI                  | 설명         | 요청                                  | 응답                                                         | 상태 코드    |
| ------ | -------------------- | ---------- | ----------------------------------- | ---------------------------------------------------------- | -------- |
| POST   | /api/carts           | 장바구니 담기    | {userId, productOptionId, quantity} | {cartId}                                                   | 201, 400 |
| GET    | /api/carts?uid={uid} | 장바구니 조회    | query: uid                          | [{cartItemId, productOption: {id, color, size}, quantity}] | 200      |
| DELETE | /api/carts/{id}      | 장바구니 항목 삭제 | path: id                            | -                                                          | 204, 404 |


## 주문/결제
| 메서드  | URI               | 설명    | 요청                                                       | 응답                                  | 상태 코드         |
| ---- |-------------------| ----- | -------------------------------------------------------- | ----------------------------------- | ------------- |
| POST | /api/orders       | 주문 생성 | {userId, cartItems: [{cartItemId, quantity}], couponId?} | {orderId, status, appliedCouponId?} | 201, 400      |
| POST | /api/orders/{id}/ | 주문 결제 | {status: "PAID", paymentMethod: "CREDIT_CARD"}                           | {orderId, status}                   | 200, 402, 409 |


## 쿠폰
| 메서드  | URI                                       | 설명      | 요청                | 응답                                                             | 상태 코드    |
| ---- | ----------------------------------------- | ------- | ----------------- | -------------------------------------------------------------- | -------- |
| POST | /api/coupons/{id}/issue                   | 쿠폰 발급   | path: id, userId  | {couponId, issuedAt}                                           | 201, 409 |
| GET  | /api/coupons/my?uid={uid}&state=AVAILABLE | 내 쿠폰 조회 | query: uid, state | [{userCouponId, couponId, discountAmount, validUntil, status}] | 200      |
