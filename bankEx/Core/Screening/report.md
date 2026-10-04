---
구현 완료 리포트 — Loan Screening System
작성일: 2026-06-13

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 서비스명 | Loan Screening System |
| 목적 | 고객 신용점수·소득·부채 기준 자동 여신 심사 (승인 / 조건부승인 / 반려) |
| 백엔드 | Spring Boot 4.0.5 + Java 17 + JPA + H2(로컬) / Oracle XE(운영) |
| 프론트엔드 | React 19 + TypeScript + Vite |
| 백엔드 포트 | 8081 |
| 프론트엔드 포트 | 5174 |

---

## 2. 백엔드 (Spring Boot)

### 신규 생성 파일

┌────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────┐
│                        파일                        │                                       설명                                       │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ ScreeningApplication.java                          │ 스프링 부트 진입점                                                               │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ config/CorsConfig.java                             │ http://localhost:5174 CORS 허용                                                  │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ config/DataInitializer.java                        │ 앱 시작 시 테스트 유저(test01)·신용정보·대출상품 3개 자동 시딩                   │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ entity/UserInfo.java                               │ 회원 JPA 엔티티 (USER_ID PK, PASSWORD, USER_NAME, BIRTH_DATE, PHONE, CREATED_AT) │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ entity/CustomerCreditInfo.java                     │ 신용·소득 JPA 엔티티 (신용점수, 연소득, 고용형태, 재직기간, 기존부채 등)         │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ entity/LoanProduct.java                            │ 대출 상품 JPA 엔티티 (상품명, 유형, 금리범위, 한도, 기간, USE_YN)               │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ entity/LoanApplication.java                        │ 대출 신청 JPA 엔티티 (신청금액, 상태코드, 신청일)                                │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ entity/LoanScreeningResult.java                    │ 심사 결과 JPA 엔티티 (CSS점수, DSR, 승인금액, 금리, 결과상태, 반려사유)          │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ repository/UserInfoRepository.java                 │ JPA Repository (findById)                                                        │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ repository/CustomerCreditInfoRepository.java       │ JPA Repository (findByUserId)                                                    │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ repository/LoanProductRepository.java              │ JPA Repository (findByUseYn)                                                     │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ repository/LoanApplicationRepository.java          │ JPA Repository (findByUserId)                                                    │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ repository/LoanScreeningResultRepository.java      │ JPA Repository (findByApplicationId)                                             │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/SignupRequest.java                             │ 회원가입 요청 DTO                                                                │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/LoginRequest.java / LoginResponse.java         │ 로그인 요청/응답 DTO                                                             │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/UserProfileResponse.java                       │ 내 정보(기본+신용) 응답 DTO                                                      │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/LoanProductResponse.java                       │ 대출 상품 응답 DTO                                                               │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/LoanApplicationRequest.java                    │ 대출 신청 요청 DTO (@Valid 검증 포함)                                            │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/LoanApplicationResponse.java                   │ 대출 신청 응답 DTO                                                               │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ dto/ScreeningResponse.java                         │ 심사 결과 응답 DTO (CSS점수, DSR, 결과상태, 승인금액, 금리 포함)                 │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ engine/LoanScreeningEngine.java                    │ 심사 엔진 핵심 로직 (신용점수·소득·재직기간·DSR 기반 CSS 산출 및 결과 판단)      │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ engine/ScreeningResult.java                        │ 심사 엔진 내부 결과 객체                                                         │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ service/UserService.java                           │ 회원가입, 로그인, 내 정보 조회 로직                                              │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ service/LoanProductService.java                    │ 대출 상품 목록/상세 조회 로직                                                    │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ service/LoanApplicationService.java                │ 대출 신청·심사 실행·결과 조회·내 내역 조회 로직 + CSS 기반 금리 산정             │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ controller/UserController.java                     │ 회원 API 엔드포인트                                                              │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ controller/LoanProductController.java              │ 대출 상품 API 엔드포인트                                                         │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ controller/LoanApplicationController.java          │ 대출 신청·심사 API 엔드포인트                                                    │
└────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────────────┘

---

## 3. API 엔드포인트 전체 목록

```
POST /api/screening/users/signup                        회원가입
POST /api/screening/users/login                         로그인
GET  /api/screening/users/{userId}/profile              내 정보(신용·소득 포함) 조회

GET  /api/screening/products                            대출 상품 목록 (USE_YN=Y)
GET  /api/screening/products/{productId}                대출 상품 상세

POST /api/screening/applications                        대출 신청
POST /api/screening/applications/{applicationId}/screening   심사 실행
GET  /api/screening/applications/my/{userId}            내 대출 신청 목록
GET  /api/screening/applications/{applicationId}/result 심사 결과 조회
```

---

## 4. 심사 엔진 로직 요약

### CSS 점수 산출 기준

| 항목 | 조건 | 점수 |
|------|------|------|
| 신용점수 | 900 이상 | +40 |
| 신용점수 | 800 이상 | +30 |
| 신용점수 | 700 이상 | +20 |
| 연소득 | 6,000만 이상 | +25 |
| 연소득 | 4,000만 이상 | +20 |
| 연소득 | 3,000만 이상 | +10 |
| 재직기간 | 36개월 이상 | +20 |
| 재직기간 | 12개월 이상 | +10 |
| DSR | 40% 이하 | +15 |
| DSR | 50% 이하 | +5 |

### 판단 기준

```
신용점수 < 700          → REJECTED  (사유: 신용점수 기준 미달)
DSR > 50%              → REJECTED  (사유: DSR 기준 초과)
CSS ≥ 80 & DSR ≤ 40%  → APPROVED
CSS ≥ 60 & DSR ≤ 50%  → CONDITIONAL (승인금액 70% 감액)
그 외                  → REJECTED  (사유: CSS 점수 기준 미달)
```

### 금리 산정 기준 (CSS 점수 기반)

| CSS 점수 | 적용 금리 |
|---------|--------|
| 90 이상 | 3.50% |
| 80 이상 | 4.20% |
| 70 이상 | 5.50% |
| 그 외   | 7.00% |

---

## 5. 프론트엔드 (React + TypeScript)

### 신규 생성 파일

┌───────────────────────────────┬──────────────────────────────────────────────────────────────────────────┐
│              파일             │                                   설명                                   │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ api/axios.ts                  │ Axios 인스턴스 (baseURL: http://localhost:8081/api/screening)             │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ api/screening_api.ts          │ 전체 API 함수 + TypeScript 인터페이스 정의                               │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ router/path.ts                │ 라우트 경로 상수 정의                                                    │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ router/index_router.tsx       │ BrowserRouter + 전체 라우트 등록                                        │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/Home.tsx                │ 홈 화면 (로그인/회원가입/대출상품 진입)                                  │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/Login.tsx               │ 로그인 페이지 (userId/password → sessionStorage 저장)                    │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/Signup.tsx              │ 회원가입 페이지 (아이디/비밀번호/이름/휴대폰)                            │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/LoanProducts.tsx        │ 대출 상품 목록 (상품 타입별 배지, 금리·한도·기간 표시, 신청하기 버튼)    │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/LoanApplication.tsx     │ 대출 신청 페이지 (금액 입력 → 신청 + 심사 자동 실행 → 결과 페이지 이동) │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/ScreeningResult.tsx     │ 심사 결과 페이지 (승인/조건부/반려 색상 구분, CSS·DSR·금리·금액 표시)    │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/MyApplications.tsx      │ 내 신청 목록 (상태 배지, 결과보기 버튼)                                  │
├───────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ pages/MyPage.tsx              │ 내 정보 (기본정보 + 신용/소득 정보, 로그아웃)                            │
└───────────────────────────────┴──────────────────────────────────────────────────────────────────────────┘

### 라우트 구성

```
/                   홈
/login              로그인
/signup             회원가입
/products           대출 상품 목록
/apply/:productId   대출 신청
/result/:applicationId  심사 결과
/my-applications    내 신청 목록
/my-page            내 정보
```

---

## 6. 검증 결과

| 항목 | 명령 | 결과 |
|------|------|------|
| Java 컴파일 | `./gradlew compileJava` | ✅ BUILD SUCCESSFUL |
| Spring Boot 통합 테스트 | `./gradlew test` | ✅ BUILD SUCCESSFUL (H2 DDL 생성/삭제 정상) |
| TypeScript 타입 검사 | `npx tsc --noEmit` | ✅ 오류 없음 |
| ESLint | `npm run lint` | ✅ 경고/오류 없음 |

---

## 7. DB 테이블 DDL (Oracle 운영 환경용)

```sql
-- 1. 회원 정보
CREATE TABLE USER_INFO (
    USER_ID        VARCHAR2(50)  PRIMARY KEY,
    PASSWORD       VARCHAR2(100) NOT NULL,
    USER_NAME      VARCHAR2(50)  NOT NULL,
    BIRTH_DATE     DATE,
    PHONE_NUMBER   VARCHAR2(20),
    CREATED_AT     DATE DEFAULT SYSDATE
);

-- 2. 고객 신용/소득 정보
CREATE TABLE CUSTOMER_CREDIT_INFO (
    CREDIT_ID          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
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

-- 3. 대출 상품
CREATE TABLE LOAN_PRODUCT (
    PRODUCT_ID         NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    PRODUCT_NAME       VARCHAR2(100) NOT NULL,
    PRODUCT_TYPE       VARCHAR2(30)  NOT NULL,
    MIN_INTEREST_RATE  NUMBER(5,2),
    MAX_INTEREST_RATE  NUMBER(5,2),
    MAX_LIMIT_AMOUNT   NUMBER,
    LOAN_PERIOD_MONTHS NUMBER,
    USE_YN             CHAR(1) DEFAULT 'Y'
);

-- 4. 대출 신청
CREATE TABLE LOAN_APPLICATION (
    APPLICATION_ID     NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    USER_ID            VARCHAR2(50) NOT NULL,
    PRODUCT_ID         NUMBER NOT NULL,
    REQUEST_AMOUNT     NUMBER NOT NULL,
    APPLICATION_STATUS VARCHAR2(30) DEFAULT 'APPLIED',
    CREATED_AT         DATE DEFAULT SYSDATE
);

-- 5. 심사 결과
CREATE TABLE LOAN_SCREENING_RESULT (
    SCREENING_ID    NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    APPLICATION_ID  NUMBER NOT NULL,
    CSS_SCORE       NUMBER,
    DSR_RATE        NUMBER(5,2),
    APPROVED_AMOUNT NUMBER,
    INTEREST_RATE   NUMBER(5,2),
    RESULT_STATUS   VARCHAR2(30),
    REJECT_REASON   VARCHAR2(500),
    CREATED_AT      DATE DEFAULT SYSDATE
);
```

---

## 8. Oracle 운영 환경 전환 시 설정

`application-local.yml`을 Oracle 설정으로 교체:

```yaml
spring:
  datasource:
    driver-class-name: oracle.jdbc.OracleDriver
    url: jdbc:oracle:thin:@localhost:1521/XE
    username: hanati
    password: (비밀번호)
  jpa:
    hibernate:
      ddl-auto: none        # 위 DDL로 직접 생성했으므로 none
    show-sql: true
    database-platform: org.hibernate.dialect.OracleDialect
  mybatis:
    mapper-locations: classpath:mapper/*.xml
```

---

## 9. 테스트 계정 (로컬 H2 자동 시딩)

| 항목 | 값 |
|------|----|
| 아이디 | test01 |
| 비밀번호 | 1234 |
| 신용점수 | 820점 |
| 연소득 | 6,500만원 |
| 고용형태 | 정규직 |
| 재직기간 | 48개월 |
| 연간 기존 상환액 | 1,000만원 |

→ 해당 조건으로 신청 시 **CSS 55~70점 / DSR 27~30%** 범위로 CONDITIONAL 또는 APPROVED 결과 확인 가능

---

## 10. 다음 단계 개선 고려사항

1. 비밀번호 평문 저장 → BCrypt 해싱 적용
2. 세션 관리 → JWT 토큰 인증 도입
3. 고객 신용정보 직접 입력 API 추가 (현재 DataInitializer로만 주입)
4. 대출 실행(EXECUTED) 처리 API 구현
5. MyBatis Mapper 활용 (복잡한 심사 이력 조회 등)

---
