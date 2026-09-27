# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Layout

**This is a multi-project monorepo, not a single app.** The git root is the parent
directory (`sideProject/`), but all working code lives under `sideProject/bankEx/`,
which is the effective project root and where this file sits.

```
bankEx/                      ← effective root (you are here)
├─ bankEx/                   1. Main banking app      (Spring Boot :8080 + React :5173)
├─ Screening/                2. Loan screening system (Spring Boot :8081 + React :5174)
├─ 대환/                      3. Loan refinancing      (Spring Boot :8082 + React :5175)
├─ bankExNative/             4. Android WebView shell (Kotlin + Compose)
├─ SystemInspectorEx/        5. Code-inspector MCP server (TypeScript)
├─ 명세/                      Development specs (source of truth for new work)
└─ report/                   Dated implementation reports
```

Projects 1–3 are **independent Spring Boot applications** with separate ports and
separate in-memory databases. They do not call each other. Shared conventions were
copied, not extracted into a library — `bankEx` is the original, `Screening` and
`대환` followed its patterns.

All three backends use `group = com.hanati.bank`, Spring Boot 4.0.5, Java 17, Gradle,
Lombok, JPA + MyBatis, and H2 (local) / Oracle (prod). All three frontends use
React 19 + TypeScript + Vite + `react-router-dom` v7 + Axios.

⚠️ There is a stray `package.json` + `node_modules` at the **git root**
(`sideProject/`, one level above this file) holding `axios`, `react-router-dom`,
`lucide-react`, and `tree`. No project here depends on it — Node and TypeScript resolve
modules by walking up the tree, so it can silently satisfy a dependency a project forgot
to declare (this already happened once with `axios` in `bankEx_Front`). If a build works
locally but fails on a clean `npm ci`, check whether the package is declared in the
project's own `package.json`.

---

## Commands

Each backend and frontend is its own build. `cd` into the directory first.

### Backends
| Project | Directory | Port |
|---|---|---|
| bankEx | `bankEx/backend/bankEx/` | 8080 |
| Screening | `Screening/backend/screening/` | 8081 |
| 대환 | `대환/backend/refinance/` | 8082 |

```bash
./gradlew bootRun         # start server
./gradlew build           # build JAR
./gradlew test            # run all tests
./gradlew compileJava     # fast compile check (used between phases)
./gradlew test --tests "com.hanati.bank.bankEx.deposit.transfer.TransferFlowTests"
```

### Frontends
| Project | Directory | Port |
|---|---|---|
| bankEx | `bankEx/front/bankEx_Front/` | 5173 (Vite default, `host: true`) |
| Screening | `Screening/front/screening_front/` | 5174 |
| 대환 | `대환/front/refinance_front/` | 5175 |

```bash
npm run dev      # dev server
npm run build    # tsc -b && vite build
npm run lint     # eslint
npm run preview  # preview production build
npx tsc -b       # type-check only (used between phases)
```

### Android (`bankExNative/`)
```bash
./gradlew assembleDebug   # build APK
./gradlew test            # unit tests
```

### MCP server (`SystemInspectorEx/project-code-inspector-mcp/`)
```bash
npm run build
```

---

## Database

**Local profile is H2 in-memory, not Oracle.** `application.yml` sets
`spring.profiles.active: local` in all three backends, and each `application-local.yml`
points at `jdbc:h2:mem:<name>` with `ddl-auto: create-drop` and the H2 console enabled.
Data does not survive a restart; `DataInitializer` seeds demo rows on boot where present.

Oracle is only wired up for `bankEx`'s `prod` profile
(`application-prod.yml` → `jdbc:oracle:thin:@localhost:1521/XE`, schema `hanati`,
`ddl-auto: validate`).

Both ORMs are used side by side in `bankEx`, split by domain:
- **JPA** — `login`, `deposit/general`, `deposit/transfer`
- **MyBatis** — `deposit/savings`, `deposit/fixed`, `loan/general`, `loan/jeonse`
  (XML in `src/main/resources/mapper/<domain>/`, `map-underscore-to-camel-case: true`)

---

## Project 1: `bankEx/` — Main banking app

Base path `/api/bank/user`. Package root `com.hanati.bank.bankEx`.

| Domain | Package | Endpoints |
|---|---|---|
| Login | `login/` | `POST /login`, `POST /signup`, `GET /profile` |
| Accounts (수신 일반) | `deposit/general/` | `GET /accounts`, `GET /accounts/{accountNumber}`, `POST /accounts`, `POST /accounts/{accountNumber}/close` |
| Transactions | `deposit/general/` | `POST /accounts/{n}/deposit`, `POST /accounts/{n}/withdraw`, `GET /accounts/{n}/transactions` |
| Savings (적금) | `deposit/savings/` | `/deposit/savings/products`, `/accounts`, `/accounts/{no}`, `/accounts/{no}/payment`, `/accounts/{no}/cancel`, `/scheduler/auto-transfer`, `/scheduler/maturity` |
| Fixed deposit (정기예금) | `deposit/fixed/` | `/fixed-deposits/products`, `POST /fixed-deposits`, `/fixed-deposits/{depositAccountId}`, `/{depositAccountId}/maturity-preview`, `/{depositAccountId}/terminate`, `/scheduler/maturity` |
| Transfer (이체) | `deposit/transfer/` | `/transfers/accounts/{n}/holder`, `POST /transfers`, `GET /transfers/{id}` |
| Loans (여신 일반) | `loan/general/` | `/loan/products`, `/loan/products/{id}`, `/loan/apply`, `/loan/applications`, `/loan/validate-customer-account`, `/loan/applications/{id}/approve`, `/loan/disbursement`, `/loan/repayment` |
| Loan repayment | `loan/general/` | `/loans/{id}/repay`, `/loans/{id}/repayment-preview`, `/loans/{id}/repayment-history` |
| Jeonse loans (전세대출) | `loan/jeonse/` | `/loan/jeonse/products`, `/loan/jeonse/applications`, `/loan/jeonse/applications/{id}/review`, `/loan/jeonse/applications/{id}/execute` |

Each domain package is flat: `controller/ service/ dto/ (entity|domain)/ (repository|mapper)/ enums/`.

`common/` holds cross-cutting pieces:
- `exception/` — `BusinessException`, `ErrorCode` (single large enum of Korean messages), `GlobalExceptionHandler`
- `response/ApiErrorResponse` — `{ success: false, code, message }`
- `util/` — `AccountNoGenerator`, `CustomerNoGenerator`, `TransactionNoGenerator`,
  `JeonseApplicationNoGenerator`, `JeonseContractNoGenerator`, `NameMaskUtil`

Frontend (`bankEx/front/bankEx_Front/src/`):
- `api/axios.ts` — `baseURL: http://${window.location.hostname}:8080/api/bank/user`
  (hostname is dynamic so the Android WebView can reach it), 5s timeout
- `api/bank_api.ts` — typed request/response interfaces per endpoint
- `router/path.ts` (constants) + `router/index_router.tsx` (BrowserRouter)
- `pages/` is nested by domain: `common/`, `login/`, `deposit/general/`,
  `loan/general/`, `loan/jeonse/`
- 8 routes: Login, Signup, Home, MyPage, Loan, JeonseLoan, MyLoans, AccountDetail

Tests: 16 files under `src/test/`, mixing flow/integration tests
(`TransferFlowTests`, `SavingsFlowTests`, `AccountCloseFlowTests`, `JeonseLoanFlowTests`,
`TransferConcurrencyTests`) with unit tests on calculators and services.

---

## Project 2: `Screening/` — Loan screening system

Base path `/api/screening`. Package root `com.hanati.bank.screening`.

- `POST /users/signup`, `POST /users/login`, `GET /users/{userId}/profile`
- `GET /products`, `GET /products/{productId}`
- `POST /applications`, `POST /applications/{id}/screening`,
  `GET /applications/my/{userId}`, `GET /applications/{id}/result`

Uses a **flat package layout** (`controller/ service/ entity/ dto/ repository/ engine/ config/`)
rather than per-domain packages. `engine/LoanScreeningEngine` holds the screening rules.
`config/` has `CorsConfig`, `SecurityConfig`, `DataInitializer`; password hashing uses
`spring-security-crypto` (no full Spring Security web stack).

Frontend uses **Zustand** stores (`authStore`, `loanProductStore`, `loanApplicationStore`,
`myApplicationStore`) and 8 pages. `api/axios.ts` sets `withCredentials: true`.

⚠️ Backend test coverage is one context-load test (`ScreeningApplicationTests`).

---

## Project 3: `대환/` — Loan refinancing (newest, 2026-08-12)

Base path `/api`. Package root `com.hanati.bank.refinance`. See `report/20260812.md`
for the full build log and `명세/대환.md` for the spec (39 sections).

Packages:
- `refinance/` — the core domain: `domain/` (status machine), `entity/`, `repository/`,
  `calculator/`, `service/` (10), `controller/` (10), `mapper/`
- `customer/` — customer lookup (`GET /customers`, `/customers/{id}`)
- `loan/` — existing-loan lookup incl. other banks (`/customers/{id}/loans`, `/loans/{id}`)
- `gateway/` — `LoanExecutionGateway` / `LoanRepaymentGateway` interfaces + `Mock*` impls
  and `DemoScenarioAccounts` (simulates other-bank execution and repayment)
- `operator/` — deliberately lightweight role model
- `audit/` — `TB_AUDIT_LOG`

Refinance endpoints: `POST /refinance/eligibility`, `POST /refinance/repayment-inquiry`,
`/refinance/applications` (create/list/detail), `/{id}/review`, `/{id}/approve`,
`/{id}/reject`, `/{id}/execute`, `/{id}/retry`, `/{id}/history`,
`GET /refinance/dashboard`, `GET /refinance/errors`.

⚠️ **Auth is not real.** Spec section 31 was intentionally scoped down to an operator
table plus an `X-Operator-Id` request header — no login, session, or JWT. The frontend
Axios interceptor injects that header from `operatorStore`. Replace with JWT if this is
ever merged into `bankEx`.

State transitions are centralized in `refinance/domain/RefinanceStatusTransition`
(guarded by `RefinanceStatusTransitionTest`) — change status rules there, not in services.

Frontend: 9 pages forming an operator workflow — OperatorSelect → Dashboard →
CustomerSearch → CustomerLoans → RefinanceWizard → ApplicationReview →
RefinanceExecution, plus FailureRetry and ApplicationHistory. Zustand stores:
`operatorStore`, `customerSearchStore`, `refinanceWizardStore`.

---

## Project 4: `bankExNative/` — Android WebView shell

Kotlin + Jetpack Compose. Wraps the `bankEx` frontend in a WebView; it contains no
banking logic of its own.

- `config/WebConfig.kt` — `BASE_URL = http://10.0.2.2:5173` (emulator's route to host
  localhost), custom user agent `BankExNative/1.0 Android`
- `webview/` — `WebViewConfigurator`, `AppWebViewClient`, `AppWebChromeClient`
- `network/NetworkChecker` + `ui/ErrorScreen` — offline handling
- `res/xml/network_security_config.xml` — permits cleartext to the dev host
- Tests: `WebViewUrlTest`, `WebViewConfiguratorTest`, `BackNavigationTest`,
  `NetworkCheckerTest`, `AppWebViewClientTest`

---

## Project 5: `SystemInspectorEx/` — Code-inspector MCP server

TypeScript MCP server (`project-code-inspector-mcp/`) that indexes and patches a
codebase. Unrelated to the banking domain.

- `tools/` — `indexProject`, `searchCode`, `readFile`, `analyzeDefect`, `proposePatch`,
  `applyPatch`, `explainPatch`, `runTests`
- `indexer/` — `projectIndexer`, `symbolExtractor`
- `security/pathGuard` — confines file access to the indexed root
- `resources/projectResources`, `utils/{fileUtils,commandRunner}`

---

## Specs and reports

`명세/` holds the development specs that drive this repo. Read the relevant one before
starting work on its domain — they are detailed enough to implement from directly.

| Spec | Lines | Status |
|---|---|---|
| `수신기본.md` | 817 | ✅ implemented (`bankEx` login + deposit/general) |
| `이체.md` | 426 | ✅ implemented (`bankEx/deposit/transfer`) |
| `전세.md` | 304 | ✅ implemented (`bankEx/loan/jeonse`) |
| `대환.md` | 1405 | ✅ implemented (`대환/`, see `report/20260812.md`) |
| `기타구현해야하는.md` | 579 | 🟡 partial (loan interest/repayment schedule done) |
| `기타구현해야하는_2.md` | 857 | 🟡 partial (§3 대출이자, §4 정기예금 done; §5 카드 / §6 외환 / §7 알림 / §8 OTP / §9 Screening 정리 not started) |
| `주담대.md` | 447 | ❌ **not started** — no mortgage/collateral code exists anywhere |

`report/` holds dated implementation reports (`20260602` → `20260812`). Each documents
what was built, decisions taken, and scope deliberately cut. Write one after completing
a spec.

---

## Conventions

**Naming is inconsistent between projects — match the file you are editing, don't normalize.**
- `bankEx` uses camelCase (lowercase-first) class names for controllers and services
  (`loginController`, `accountService`, `jeonseLoanService`) but PascalCase for entities,
  DTOs, repositories, and mappers. Newer files broke the pattern
  (`LoanRepaymentController`, `LoanRepaymentService`).
- `Screening` and `대환` use PascalCase throughout.

**Error handling** — throw `BusinessException(ErrorCode.X)`; `GlobalExceptionHandler`
maps it to `ApiErrorResponse`. Add new codes to the `ErrorCode` enum with a Korean
user-facing message. `대환` additionally has an `ApiResponse` success wrapper; `bankEx`
returns DTOs directly.

**ID generation** — hand-rolled generators in `common/util/`, one per entity type.
Follow the existing generator when adding an entity that needs a formatted number.

**Masking** — `NameMaskUtil` for customer names in responses.

**Domain enums** live in the domain's `enums/` (or `domain/` in `대환`), not in `common/`.

**Registering a new MyBatis domain** — `BankExApplication` carries an explicit
`@MapperScan({...})` listing packages one by one, so a new `@Mapper` interface is NOT
picked up automatically. Add its package there or the context fails at startup with
`No qualifying bean of type '...Mapper'`. Mapper XML under `src/main/resources/mapper/<domain>/`
is found by the `classpath:mapper/**/*.xml` glob without extra config, and new tables go in
`schema.sql` (auto-run on H2 only — mirror them into Oracle by hand).

**Injecting the business date** — services whose result depends on "today" take a
`LocalDate` parameter and let the controller pass `LocalDate.now()`
(`savingsMaturityService.execute(today)`, `autoTransferService.execute(date)`,
`FixedDepositService.terminate(id, today)`). Keeps date-dependent rules testable; don't
call `LocalDate.now()` deep inside such a service.

**Type imports** — all three frontends set `verbatimModuleSyntax: true`, so a type must be
imported as `import type { X } from '...'`. A plain `import { X }` for a type fails the
build with `TS1484`. Split the import when a module supplies both a value and a type.

**Phased verification** — the established workflow is to run `./gradlew compileJava`
(or `test`) and `npx tsc -b` at each phase boundary before moving on, as recorded in
the reports.

---
---

# Behavioral guidelines

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
