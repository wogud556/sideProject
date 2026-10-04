아래는 개인 신용대출/전세대출 심사 업무를 단순화한 개발 명세서야.

1. 개발 명세서

서비스명

Loan Screening System

목적

고객이 대출 상품을 신청하면, 시스템이 고객 정보·소득·신용점수·기존부채를 기준으로 자동 심사하여 승인/반려/조건부승인을 판단한다.

⸻

2. 주요 업무 흐름

회원가입
  ↓
로그인
  ↓
대출상품 조회
  ↓
대출 신청
  ↓
고객 정보 수집
  ↓
심사 엔진 실행
  ↓
심사 결과 저장
  ↓
승인 / 조건부승인 / 반려
  ↓
대출 실행

⸻

3. 기능 명세

3-1. 회원 기능

기능	설명
회원가입	고객 기본정보 등록
로그인	사용자 인증
내 정보 조회	소득, 직장, 신용점수 조회

⸻

3-2. 대출상품 기능

기능	설명
대출상품 목록 조회	신용대출, 전세대출 등
상품 상세 조회	금리, 한도, 기간 확인
상품 신청	고객이 원하는 금액 입력

⸻

3-3. 심사 기능

기능	설명
자동 심사	CSS 점수 계산
DSR 계산	소득 대비 부채 상환 비율 계산
승인 판단	승인/조건부승인/반려 결정
심사 결과 저장	결과, 사유, 점수 저장

⸻

4. DB 테이블 설계서

4-1. USER_INFO

고객 기본 정보 테이블

CREATE TABLE USER_INFO (
    USER_ID        VARCHAR2(50) PRIMARY KEY,
    PASSWORD       VARCHAR2(100) NOT NULL,
    USER_NAME      VARCHAR2(50) NOT NULL,
    BIRTH_DATE     DATE,
    PHONE_NUMBER   VARCHAR2(20),
    CREATED_AT     DATE DEFAULT SYSDATE
);

⸻

4-2. CUSTOMER_CREDIT_INFO

고객 신용/소득 정보

CREATE TABLE CUSTOMER_CREDIT_INFO (
    CREDIT_ID          NUMBER PRIMARY KEY,
    USER_ID            VARCHAR2(50) NOT NULL,
    CREDIT_SCORE       NUMBER NOT NULL,
    ANNUAL_INCOME      NUMBER NOT NULL,
    EMPLOYMENT_TYPE    VARCHAR2(30),
    COMPANY_NAME       VARCHAR2(100),
    EMPLOYMENT_MONTHS  NUMBER,
    EXISTING_DEBT      NUMBER DEFAULT 0,
    ANNUAL_REPAYMENT   NUMBER DEFAULT 0,
    UPDATED_AT         DATE DEFAULT SYSDATE
);

⸻

4-3. LOAN_PRODUCT

대출 상품 테이블

CREATE TABLE LOAN_PRODUCT (
    PRODUCT_ID       NUMBER PRIMARY KEY,
    PRODUCT_NAME     VARCHAR2(100) NOT NULL,
    PRODUCT_TYPE     VARCHAR2(30) NOT NULL,
    MIN_INTEREST_RATE NUMBER(5,2),
    MAX_INTEREST_RATE NUMBER(5,2),
    MAX_LIMIT_AMOUNT NUMBER,
    LOAN_PERIOD_MONTHS NUMBER,
    USE_YN           CHAR(1) DEFAULT 'Y'
);

⸻

4-4. LOAN_APPLICATION

대출 신청 테이블

CREATE TABLE LOAN_APPLICATION (
    APPLICATION_ID    NUMBER PRIMARY KEY,
    USER_ID           VARCHAR2(50) NOT NULL,
    PRODUCT_ID        NUMBER NOT NULL,
    REQUEST_AMOUNT    NUMBER NOT NULL,
    APPLICATION_STATUS VARCHAR2(30) DEFAULT 'APPLIED',
    CREATED_AT        DATE DEFAULT SYSDATE
);

상태 예시:

APPLIED      신청
SCREENING    심사중
APPROVED     승인
CONDITIONAL  조건부승인
REJECTED     반려
EXECUTED     실행완료

⸻

4-5. LOAN_SCREENING_RESULT

심사 결과 테이블

CREATE TABLE LOAN_SCREENING_RESULT (
    SCREENING_ID       NUMBER PRIMARY KEY,
    APPLICATION_ID     NUMBER NOT NULL,
    CSS_SCORE          NUMBER,
    DSR_RATE           NUMBER(5,2),
    APPROVED_AMOUNT    NUMBER,
    INTEREST_RATE      NUMBER(5,2),
    RESULT_STATUS      VARCHAR2(30),
    REJECT_REASON      VARCHAR2(500),
    CREATED_AT         DATE DEFAULT SYSDATE
);

⸻

5. 심사 엔진 로직

5-1. 심사 기준 예시

항목	기준	점수
신용점수 900 이상	우수	40점
신용점수 800 이상	양호	30점
신용점수 700 이상	보통	20점
신용점수 700 미만	위험	0점
연소득 6천 이상	우수	25점
연소득 4천 이상	양호	20점
재직기간 36개월 이상	안정	20점
DSR 40% 이하	양호	15점

⸻

5-2. DSR 계산

DSR = 연간 원리금 상환액 / 연소득 × 100

예시:

연소득: 65,000,000원
기존 연간 상환액: 10,000,000원
신규 예상 연간 상환액: 8,000,000원
DSR = 18,000,000 / 65,000,000 × 100
DSR = 27.69%

⸻

6. 심사 판단 로직

if (creditScore < 700) {
    return REJECTED;
}
if (dsrRate > 50) {
    return REJECTED;
}
if (cssScore >= 80 && dsrRate <= 40) {
    return APPROVED;
}
if (cssScore >= 60 && dsrRate <= 50) {
    return CONDITIONAL;
}
return REJECTED;

⸻

7. Java 심사 엔진 예시

public class LoanScreeningEngine {
    public ScreeningResult screen(CustomerCreditInfo customer, LoanApplication application) {
        int cssScore = 0;
        // 1. 신용점수 평가
        if (customer.getCreditScore() >= 900) {
            cssScore += 40;
        } else if (customer.getCreditScore() >= 800) {
            cssScore += 30;
        } else if (customer.getCreditScore() >= 700) {
            cssScore += 20;
        }
        // 2. 연소득 평가
        if (customer.getAnnualIncome() >= 60000000) {
            cssScore += 25;
        } else if (customer.getAnnualIncome() >= 40000000) {
            cssScore += 20;
        } else if (customer.getAnnualIncome() >= 30000000) {
            cssScore += 10;
        }
        // 3. 재직기간 평가
        if (customer.getEmploymentMonths() >= 36) {
            cssScore += 20;
        } else if (customer.getEmploymentMonths() >= 12) {
            cssScore += 10;
        }
        // 4. DSR 계산
        double newAnnualRepayment = application.getRequestAmount() / 5.0;
        double totalAnnualRepayment =
                customer.getAnnualRepayment() + newAnnualRepayment;
        double dsrRate =
                totalAnnualRepayment / customer.getAnnualIncome() * 100;
        // 5. DSR 점수
        if (dsrRate <= 40) {
            cssScore += 15;
        } else if (dsrRate <= 50) {
            cssScore += 5;
        }
        // 6. 결과 판단
        String resultStatus;
        String rejectReason = null;
        long approvedAmount = application.getRequestAmount();
        if (customer.getCreditScore() < 700) {
            resultStatus = "REJECTED";
            rejectReason = "신용점수 기준 미달";
            approvedAmount = 0;
        } else if (dsrRate > 50) {
            resultStatus = "REJECTED";
            rejectReason = "DSR 기준 초과";
            approvedAmount = 0;
        } else if (cssScore >= 80) {
            resultStatus = "APPROVED";
        } else if (cssScore >= 60) {
            resultStatus = "CONDITIONAL";
            approvedAmount = application.getRequestAmount() * 70 / 100;
        } else {
            resultStatus = "REJECTED";
            rejectReason = "CSS 점수 기준 미달";
            approvedAmount = 0;
        }
        return new ScreeningResult(
                cssScore,
                dsrRate,
                approvedAmount,
                resultStatus,
                rejectReason
        );
    }
}

⸻

8. API 설계 예시

대출 신청

POST /api/loan/applications
{
  "userId": "test01",
  "productId": 1,
  "requestAmount": 80000000
}

⸻

심사 실행

POST /api/loan/applications/{applicationId}/screening

응답:

{
  "applicationId": 1001,
  "cssScore": 85,
  "dsrRate": 32.5,
  "resultStatus": "APPROVED",
  "approvedAmount": 80000000,
  "interestRate": 4.2,
  "rejectReason": null
}

⸻

9. 프로젝트 구조 예시

src/main/java/com/example/bank
 ├── controller
 │    └── LoanController.java
 ├── service
 │    └── LoanService.java
 ├── engine
 │    └── LoanScreeningEngine.java
 ├── mapper
 │    └── LoanMapper.java
 ├── dto
 │    ├── LoanApplicationRequest.java
 │    └── ScreeningResponse.java
 └── domain
      ├── UserInfo.java
      ├── CustomerCreditInfo.java
      ├── LoanProduct.java
      ├── LoanApplication.java
      └── LoanScreeningResult.java

⸻

이 정도로 구현하면 은행 여신 심사 프로세스를 단순화한 포트폴리오용 백엔드로 충분히 괜찮아.