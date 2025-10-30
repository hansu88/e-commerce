## 📋 작업 내용

### 작업 유형
- [ ] ERD 설계
- [ ] API 명세서 작성
- [ ] Sequence Diagram 작성
- [ ] Mock API 구현
- [ ] Swagger 문서화
- [ ] 기능 구현

📌 이커머스 핵심 기능 체크리스트
1. 상품 관리

- [ ] 상품 정보 조회 (가격, 재고)
- [ ] 재고 실시간 확인
- [ ] 인기 상품 통계 (최근 3일, Top 5)

API 구현
- [ ] GET /api/products → 상품 목록 조회
- [ ] GET /api/products/{id} → 상품 상세 조회
- [ ] GET /api/products/popular → 인기 상품 조회

2. 주문/결제 시스템

- [ ] 장바구니 기능
- [ ] 재고 확인 및 차감
- [ ] 잔액 기반 결제
- [ ] 쿠폰 할인 적용

API 구현
- [ ] POST /api/carts → 장바구니 담기
- [ ] GET /api/carts → 장바구니 조회
- [ ] DELETE /api/carts/{id} → 장바구니 항목 삭제
- [ ] POST /api/orders → 주문 생성
- [ ] POST /api/orders/{id}/pay → 주문 결제

3. 쿠폰 시스템
- [ ] 선착순 발급 (한정 수량)
- [ ] 쿠폰 유효성 검증
- [ ] 사용 이력 관리

API 구현
- [ ] POST /api/coupons/{id}/issue → 쿠폰 발급
- [ ] GET /api/coupons/my → 내 쿠폰 조회

4. 데이터 연동
- [ ] 주문 데이터 외부 전송
- [ ] 실패 시에도 주문은 정상 처리

API 구현
별도 API 없음 (서비스 레이어에서 처리, Mock API 구현 시 제외 가능)


5. 문서화 & 테스트 
- [ ] ERD 다이어그램 완료
- [ ] API 명세서 완료
- [ ] Sequence Diagram 작성
- [ ] Mock API 정상 응답 확인
- [ ] Swagger UI 확인
- [ ] API 명세와 응답 일치 확인
- [ ] README 업데이트