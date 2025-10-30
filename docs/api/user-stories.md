# 사용자 스토리

## 상품 관리
1. 상품 목록 조회
   - As a 사용자
   - I want 판매 중인 상품 목록을 확인
   - So that 원하는 상품을 선택할 수 있다.
   - Acceptance Criteria:
      - 판매중인 상품만 표시
      - 요청 시 정렬 및 페이지네이션 지원

2. 상품 상세 조회
   - As a 사용자
   - I want 상품 상세 정보 확인
   - So that 색상, 사이즈, 가격, 재고 확인 가능
   - Acceptance Criteria:
      - 옵션별 재고 확인 가능
      - 존재하지 않는 상품 요청 시 404 반환

3. 인기 상품 조회
   - As a 사용자
   - I want 최근 3일간 Top5 인기 상품 확인
   - So that 구매 결정에 참고
   - Acceptance Criteria:
      - 판매량 기준 정렬
      - 데이터 없으면 빈 배열 반환

## 장바구니
4. 장바구니 담기
   - As a 사용자
   - I want 상품 옵션 장바구니에 담기
   - So that 결제 준비 가능
   - Acceptance Criteria:
      - 재고 부족 시 에러 반환
      - 동일 상품 옵션 중복 담기 가능 여부 선택

5. 장바구니 조회
   - As a 사용자
   - I want 내 장바구니 목록 확인
   - So that 담은 상품과 총 금액 확인
   - Acceptance Criteria:
      - 각 상품 옵션, 가격, 수량 표시
      - 총 금액 계산

6. 장바구니 항목 삭제
   - As a 사용자
   - I want 장바구니에서 상품 삭제
   - So that 필요 없는 상품 제거
   - Acceptance Criteria:
      - 삭제 시 장바구니 총 금액 재계산

## 주문/결제
7. 주문 생성
   - As a 사용자
   - I want 장바구니 기반 주문 생성
   - So that 결제를 준비
   - Acceptance Criteria:
      - 재고 확인 후 예약/차감
      - 주문 생성 실패 시 롤백

8. 주문 결제
   - As a 사용자
   - I want 잔액/결제 수단으로 결제
   - So that 주문 상태를 PAID로 변경
   - Acceptance Criteria:
      - 잔액 부족 시 결제 실패
      - 쿠폰 적용 가능

9. 재고 차감
   - As a 시스템
   - I want 주문 완료 시 재고 차감
   - So that 다른 사용자가 구매할 수 있는 재고 관리
   - Acceptance Criteria:
      - 동시성 고려 (락 또는 예약 재고)
      - 재고 부족 시 주문 실패

## 쿠폰
10. 쿠폰 발급
   - As a 사용자
   - I want 선착순 쿠폰 발급
   - So that 주문 시 할인 적용
   - Acceptance Criteria:
      - 발급 수량 한정
      - 동시 요청 시 선착순 보장

11. 내 쿠폰 조회
   - As a 사용자
   - I want 내 쿠폰 목록 확인
   - So that 사용 가능한 쿠폰 확인

12. 쿠폰 사용 이력 확인
   - As a 시스템
   - I want 쿠폰 사용 기록 저장
   - So that 중복 사용 방지 및 감사 가능