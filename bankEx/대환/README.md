# 가계대출 대환 업무 시스템

`bankEx/명세/대환.md` 명세에 따라 개발된 은행 가계대출 대환(대환대출) 업무 시스템입니다.

`bankEx/`, `Screening/`과 같은 저장소 루트의 독립 프로젝트이며, 나중에 필요 시 패키지 단위로 `bankEx/`에 흡수될 수 있도록 그 컨벤션을 최대한 따라 구현했습니다.

## 1. 프로젝트 개요

고객의 기존 가계대출을 조회하고, 대환 가능 여부를 검증한 뒤 신규 대출을 실행하여 기존 대출을 상환하는 업무 흐름을 지원하는 은행 내부 업무 시스템입니다. 실제 금융공동망/코어뱅킹 연계가 없는 개발 환경에서도 동작하도록 외부 연계 부분은 Gateway 인터페이스 + Mock 구현체로 분리했습니다.

## 2. 기술 스택

- **Backend**: Java 17, Spring Boot 4.0.5, Spring Web/Validation, Spring Data JPA, MyBatis(복잡 조회 전용), H2(로컬/테스트), Oracle(운영)
- **Frontend**: React 19, TypeScript, Vite, React Router v7, Zustand, Axios
- **DB**: Oracle(운영) / H2(로컬 — JPA `ddl-auto: create-drop`으로 엔티티에서 자동 생성)

## 3. Architecture

```
com.hanati.bank.refinance
├─ config/         CorsConfig, DataInitializer(Mock 데이터 시딩)
├─ common/         BusinessException, ErrorCode, GlobalExceptionHandler, ApiResponse/ApiErrorResponse, 마스킹/채번 유틸
├─ operator/       경량 권한모델 (직원 + Role), OperatorAuthService
├─ customer/       고객 조회
├─ loan/           기존대출(TB_LOAN) 조회 — 타행 포함 Mock 데이터
├─ refinance/      핵심 도메인 (상태머신, 엔티티, 신청/심사/승인/실행/재처리/이력 Service, Controller)
├─ gateway/        LoanExecutionGateway / LoanRepaymentGateway 인터페이스 + Mock 구현체
└─ audit/          TB_AUDIT_LOG 감사로그
```

기존 프로젝트(`bankEx`, `Screening`)와 동일하게 **도메인별 평평한 패키지 구조**(`controller/service/entity/dto/repository`)를 사용했습니다. `RefinanceApplication` 엔티티에 `@Version`을 적용해 낙관적 락 기반 동시성 제어를 하며, 실행/재처리처럼 중요한 상태 전이는 상태조건부 UPDATE(`updateStatusIfMatch`)로 한 번 더 보호합니다.

## 4. 디렉터리 구조

```
대환/
├─ backend/refinance/     Spring Boot 백엔드 (포트 8082)
└─ front/refinance_front/ React 프론트엔드 (포트 5175)
```

## 5. 실행 방법

### Backend

```bash
cd 대환/backend/refinance
./gradlew bootRun     # http://localhost:8082, local 프로파일(H2) 기본 활성화
./gradlew test        # 단위 + 통합 테스트
```

### Frontend

```bash
cd 대환/front/refinance_front
npm install
npm run dev            # http://localhost:5175
```

프론트 실행 후 브라우저에서 `http://localhost:5175` 접속 → **직원 선택 화면**(경량 권한모델 데모용, 실제 로그인 아님)에서 역할을 선택하면 대시보드로 이동합니다.

## 6. Oracle 설정 / 테이블 생성

운영 Oracle 반영 시 `대환/backend/refinance/src/main/resources/oracle-ddl.sql`을 수동으로 실행합니다 (H2처럼 자동 실행되지 않음). `application.yml`에 `spring.datasource`를 Oracle 접속 정보로 교체하고 `ojdbc11` 드라이버(이미 `build.gradle`에 런타임 의존성으로 포함됨)를 사용하면 됩니다.

## 7. Mock 데이터

로컬(`local` 프로파일) 기동 시 `DataInitializer`가 다음을 자동 시딩합니다.

- 직원 5명 (`teller01`/`reviewer01`/`approver01`/`operator01`/`admin01`, 역할별 1명씩)
- 고객 6명 — 명세 33번의 필수 테스트 시나리오에 각각 대응:
  1. 정상 고객 (대환 가능한 대출 보유)
  2. 연체 고객 (대환 불가)
  3. 대출잔액 없음 (대환 불가)
  4. 정상 대환 데모 (신규대출 실행 + 기존대출 상환 모두 성공)
  5. 신규대출 실행 실패 데모 (`gateway.DemoScenarioAccounts.EXECUTION_FAILURE_ACCOUNT` 계좌 — Mock Gateway가 결정적으로 실행 거절)
  6. 신규대출 성공 + 기존대출 상환 실패(재처리 대상) 데모 (`EXECUTION_FAILURE_ACCOUNT`/`REPAYMENT_FAILURE_ACCOUNT` — 최초 상환 시도만 실패, 재처리 시 성공)

"중복 실행" 시나리오는 별도 데이터 없이, 승인된 신청에 대해 `/execute`를 두 번 호출해 검증할 수 있습니다(두 번째 호출은 상태조건부 UPDATE에 걸려 거부됨).

## 8. Backend 실행 방법 (개발 확인용 curl 예시)

```bash
# 고객 검색
curl -H "X-Operator-Id: teller01" "http://localhost:8082/api/customers?name=홍정상"

# 대환 신청 -> 심사 -> 승인 -> 실행
curl -X POST http://localhost:8082/api/refinance/applications -H "Content-Type: application/json" -H "X-Operator-Id: teller01" -d '{...}'
curl -X POST http://localhost:8082/api/refinance/applications/{id}/review -H "X-Operator-Id: reviewer01" -d '{"opinion":"..."}'
curl -X POST http://localhost:8082/api/refinance/applications/{id}/approve -H "X-Operator-Id: approver01" -d '{}'
curl -X POST http://localhost:8082/api/refinance/applications/{id}/execute -H "X-Operator-Id: operator01"

# 실패 시 재처리
curl -X POST http://localhost:8082/api/refinance/applications/{id}/retry -H "X-Operator-Id: operator01"
```

## 9. 주요 API

| 구분 | API |
|---|---|
| 고객 | `GET /api/customers`, `GET /api/customers/{id}` |
| 기존대출 | `GET /api/customers/{id}/loans`, `GET /api/loans/{id}` |
| 대환가능여부 | `POST /api/refinance/eligibility` |
| 상환금액조회 | `POST /api/refinance/repayment-inquiry` |
| 신청 | `POST/GET /api/refinance/applications`, `GET /api/refinance/applications/{id}` |
| 심사 | `POST /api/refinance/applications/{id}/review` |
| 승인/거절 | `POST /api/refinance/applications/{id}/approve`, `/reject` |
| 실행 | `POST /api/refinance/applications/{id}/execute` |
| 재처리 | `POST /api/refinance/applications/{id}/retry` |
| 실패거래 조회 | `GET /api/refinance/errors` (거래일자/신청번호/고객번호/거래유형/오류코드/처리상태 다중조건) |
| 업무이력 | `GET /api/refinance/applications/{id}/history` |
| Dashboard | `GET /api/refinance/dashboard` |

승인/실행/재처리 등 업무 API는 `X-Operator-Id` 헤더로 직원을 식별하고 역할(Role)을 검증합니다.

## 10. 업무 흐름

```
고객조회 → 기존대출조회 → 대환대상대출선택 → 대환가능여부검증 → 기존대출상환예정금액조회
→ 신규대출조건입력 → 대환금액산정 → 신청등록 → 심사 → 승인 → 신규대출실행 → 기존대출상환 → 대환완료
```

## 11. 상태 정의

`DRAFT → REQUESTED → REVIEWING → (APPROVED | REJECTED) → EXECUTING → NEW_LOAN_EXECUTED → REPAYING → COMPLETED`, 실패 시 `FAILED`, 취소 시 `CANCELLED`.

허용된 전이만 `RefinanceStatusTransition`에 화이트리스트로 정의되어 있으며, `FAILED`에서의 재처리는 실패 단계(`TB_REFINANCE_ERROR.failedStep`)에 따라 `EXECUTING`(신규대출 미실행) 또는 `REPAYING`(신규대출은 이미 실행됨, 상환만 재시도)으로만 분기합니다 — **신규대출 재실행은 절대 발생하지 않습니다.**

## 12. 알려진 스코프 축소 (명세 대비)

- **권한관리(명세 31번)**: 실제 Spring Security 인증(로그인/세션/JWT)이 아닌, `TB_REFINANCE_OPERATOR` + `X-Operator-Id` 헤더 기반 경량 역할 검증입니다. bankEx 흡수 시 JWT 기반으로 교체가 필요합니다.
- **신청 수정(`PUT /applications/{id}`)**: 명세 25번 API 목록에 있으나 이번 구현 범위에서는 제외했습니다.
- 상환예정금액의 경과이자/중도상환수수료 산정식은 실제 은행 고지 방식이 아닌 간이 산정식입니다(`RepaymentAmountCalculator` 주석 참고).

## 13. 테스트

`./gradlew test`로 실행되는 15개 테스트 중 특히 다음이 명세 34번의 핵심 시나리오를 검증합니다.

- `RefinanceRetryFlowTest`: 신규대출 실행 성공 → 기존대출 상환 실패 → 재처리 → 상환 성공 → `COMPLETED`. 재처리 전후로 `TB_REFINANCE_TRANSACTION`의 `NEW_LOAN_EXECUTION` 거래가 정확히 1건임을 assert하여 신규대출 중복 실행이 없음을 증명합니다.
- `RefinanceHappyPathFlowTest`: 정상 플로우 전체 + 완료 후 재실행 요청이 차단되는지(중복 실행 방지) 검증합니다.
