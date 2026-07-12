# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BankEx is a full-stack banking web application simulating KB국민은행 features (login, accounts, transactions, loans). Stack: React 19 + TypeScript + Vite (frontend) and Spring Boot 4.0.5 + Java 17 (backend) with Oracle DB.

## Commands

### Frontend (`bankEx/front/bankEx_Front/`)
```bash
npm run dev      # dev server at http://localhost:5173
npm run build    # tsc -b && vite build
npm run lint     # eslint
npm run preview  # preview production build
```

### Backend (`bankEx/backend/bankEx/`)
```bash
./gradlew bootRun        # start server at http://localhost:8080
./gradlew build          # build JAR
./gradlew test           # run all tests
./gradlew test --tests "com.hanati.bank.bankEx.SomeTest"  # run single test
```

## Architecture

### Frontend structure
- `src/api/axios.ts` — Axios instance with `baseURL: http://localhost:8080/api/bank/user`
- `src/api/bank_api.ts` — API call functions (typed request/response interfaces)
- `src/router/path.ts` — Route path constants; `src/router/index_router.tsx` — BrowserRouter setup
- `src/pages/` — Page-level components (Home, Login, MyPage, loan)
- `src/components/` — Reusable UI components

> Note: `loan.tsx` lives in `src/pages/` but is not yet registered in the router. It also imports from `./components/` (should be `../components/`).

### Backend structure
- Base API path: `/api/bank/user`
- Three domain controllers: `loginController`, `accountController`, `transController`
- Corresponding services: `loginService`, `accountService`, `transService`
- Only `loginController` has an implemented endpoint (`POST /login`, stub only)
- `accountController` and `transController` are empty scaffolds

### Database
- Oracle XE at `localhost:1521/XE`, schema: `hanati`
- Dual ORM: JPA + MyBatis (both configured via `spring-boot-starter-data-jpa` and `mybatis-spring-boot-starter`)
- Config: `bankEx/backend/bankEx/src/main/resources/application.yml`


# CLAUDE.md

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