# 데이터 모델 / ERD (최신)

## 엔티티 목록

### Product
id (PK)  
name  
price  
status (ACTIVE / INACTIVE)  
createdAt  
관계: 1:N ProductOption

### ProductOption
id (PK)  
productId (FK → Product)  
color  
size  
stock  
관계: 1:N CartItem, 1:N OrderItem, 1:N StockHistory

### Cart
id (PK)  
userId  
createdAt  
관계: 1:N CartItem

### CartItem
id (PK)  
cartId (FK → Cart)  
productOptionId (FK → ProductOption)  
quantity

### Order
id (PK)  
userId  
status (CREATED, PAID, CANCELLED)  
totalAmount  
createdAt  
관계: 1:N OrderItem

### OrderItem
id (PK)  
orderId (FK → Order)  
productOptionId (FK → ProductOption)  
quantity  
price
createdAt

### Coupon
id (PK)  
code  
discountAmount  
totalQuantity  
issuedQuantity  
validFrom  
validUntil  
관계: 1:N UserCoupon

### UserCoupon
id (PK)  
userId  
couponId (FK → Coupon)  
issuedAt  
usedAt  
status (AVAILABLE, USED, EXPIRED)

### StockHistory
id (PK)  
productOptionId (FK → ProductOption)  
changeQty (증가/감소)  
reason (ORDER, CANCEL, RESTOCK 등)  
createdAt

## 관계 요약
Product 1:N ProductOption  
ProductOption 1:N CartItem, 1:N OrderItem, 1:N StockHistory  
Cart 1:N CartItem  
Order 1:N OrderItem  
Coupon 1:N UserCoupon  
UserCoupon → Order (Optional, 주문 시 쿠폰 적용 가능)