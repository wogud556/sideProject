
현재 개발되어있는 bankEx/bankEx/front, bankEx/bankEx/bankend 경로에 기존 대출 업무상에 아래와 같은 명세로 개발 수행해줘

3. 일반대출 이자 계산 보완

3.1 목적

현재 일반대출 상환 시 원금만 차감되며 상품의 interestRate가 실제 계산에 사용되지 않는다.

이를 개선하여 최소한 다음 정보를 계산할 수 있도록 한다.

- 월 납부 이자
- 원금 상환액
- 총 상환액
- 상환 후 잔여 원금

⸻

3.2 초기 지원 상환 방식

프로토타입에서는 다음 두 방식만 우선 지원한다.

EQUAL_PRINCIPAL
- 원금균등상환
BULLET
- 만기일시상환

원리금균등상환은 계산 구조를 확장할 수 있도록 인터페이스만 고려하고, 초기 구현에서는 제외 가능하다.

⸻

3.3 핵심 계산

월 이자율 = 연 이자율 / 12
월 이자 = 상환 전 잔여 원금 × 월 이자율

금액 계산은 반드시 BigDecimal을 사용한다.

⸻

3.4 상환 처리

기존 repay()를 다음 순서로 변경한다.

1. 대출 계약 조회
2. 상품 금리 또는 계약 적용금리 조회
3. 현재 회차 이자 계산
4. 납부금액을 이자와 원금으로 분리
5. 잔여 원금 차감
6. 상환 내역 저장
7. 거래내역 생성
8. 잔여 원금이 0원이면 대출 완료 처리

상환 응답 예시:

{
  "loanId": 1001,
  "paymentAmount": 520000,
  "principalAmount": 480000,
  "interestAmount": 40000,
  "remainingPrincipal": 9520000,
  "status": "ACTIVE"
}

⸻

3.5 주요 클래스

LoanInterestCalculator
LoanRepaymentService
LoanRepaymentHistory
LoanRepaymentType

⸻

3.6 API

POST /api/loans/{loanId}/repay
GET  /api/loans/{loanId}/repayment-preview
GET  /api/loans/{loanId}/repayment-history

⸻

planner 에이전트로 요구사항을 분석하고,

developer 에이전트로 구현하고,

reviewer 에이전트로 코드 리뷰하고,

tester 에이전트로 테스트 작성 및 실행까지 진행해줘.

