# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BankEx is a full-stack banking web application simulating KB국민은행 features (login, accounts, transactions, loans). Stack: React 19 + TypeScript + Vite (frontend) and Spring Boot 4.0.5 + Java 17 (backend) with Oracle DB.

## Commands

### Frontend (`front/bankEx_Front/`)
```bash
npm run dev      # dev server at http://localhost:5173
npm run build    # tsc -b && vite build
npm run lint     # eslint
npm run preview  # preview production build
```

### Backend (`backend/bankEx/`)
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
- Config: `backend/bankEx/src/main/resources/application.yml`
