---
보안 검토 및 수정 리포트
작성일: 2026-06-13

---

## 1. 검토 개요

Screening 프로젝트 전체 소스코드(백엔드 Java, 프론트엔드 TypeScript)를 대상으로
보안 취약점을 점검하고 주요 항목을 수정하였다.

---

## 2. 발견된 보안 이슈 전체 목록

| 번호 | 항목 | 심각도 | 로컬 영향 | 운영 영향 |
|------|------|--------|----------|----------|
| 1 | 비밀번호 평문 저장 | 🔴 HIGH | 있음 | 매우 큼 |
| 2 | API 인증/인가 없음 | 🔴 HIGH | 있음 | 매우 큼 |
| 3 | 로그인 에러 메시지로 아이디 존재 여부 노출 | 🟡 MEDIUM | 낮음 | 있음 |
| 4 | sessionStorage에 사용자 정보 저장 | 🟡 MEDIUM | 낮음 | 있음 |
| 5 | H2 콘솔 활성화 | 🟢 LOW | 없음 | 큼 |
| 6 | SQL 로그 노출 (show-sql: true) | 🟢 LOW | 없음 | 있음 |
| 7 | 테스트 계정 하드코딩 (운영 환경 미제한) | 🟢 LOW | 없음 | 있음 |

---

## 3. 수정 완료 항목

### 3-1. 비밀번호 BCrypt 해싱 적용 🔴 → ✅

**문제**

비밀번호가 평문 그대로 DB에 저장되고, 로그인 시 문자열 `equals()`로 비교하였다.
DB가 유출될 경우 모든 계정의 비밀번호가 즉시 노출된다.

```java
// 수정 전 — UserService.java
.password(req.getPassword())                          // 평문 저장
user.getPassword().equals(req.getPassword())          // 평문 비교
```

**수정 내용**

`spring-security-crypto` 의존성을 추가하고 `BCryptPasswordEncoder`를 적용하였다.
Spring Security 전체 프레임워크가 아닌 암호화 모듈만 추가하여 기존 엔드포인트에
영향을 주지 않도록 하였다.

```java
// 수정 후 — UserService.java
.password(passwordEncoder.encode(req.getPassword()))  // BCrypt 해싱 저장
passwordEncoder.matches(req.getPassword(), user.getPassword())  // 해시 비교
```

**수정 파일**

| 파일 | 변경 내용 |
|------|----------|
| `build.gradle` | `spring-security-crypto` 의존성 추가 |
| `config/SecurityConfig.java` | `BCryptPasswordEncoder` Bean 신규 등록 |
| `service/UserService.java` | `encode()` / `matches()` 적용 |
| `config/DataInitializer.java` | 테스트 계정 비밀번호도 해싱 처리 |

---

### 3-2. 로그인 에러 메시지 통일 (사용자 열거 공격 방지) 🟡 → ✅

**문제**

아이디가 없을 때와 비밀번호가 틀렸을 때 에러 메시지가 달라
공격자가 유효한 아이디 목록을 수집할 수 있었다 (사용자 열거 공격, User Enumeration).

```java
// 수정 전 — UserService.java
"존재하지 않는 아이디입니다."   // 아이디 존재 여부 노출
"비밀번호가 올바르지 않습니다."
```

**수정 내용**

두 경우를 하나의 메시지로 통일하고, 아이디 조회 실패 시에도
즉시 예외를 던지지 않고 `orElse(null)` 로 처리하여 분기를 단일화하였다.

```java
// 수정 후 — UserService.java
UserInfo user = userInfoRepository.findById(req.getUserId()).orElse(null);
if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
    throw new IllegalArgumentException("아이디 또는 비밀번호를 확인해 주세요.");
}
```

**수정 파일**

| 파일 | 변경 내용 |
|------|----------|
| `service/UserService.java` | 로그인 에러 메시지 단일화 |

---

### 3-3. DataInitializer 로컬 환경 전용 제한 🟢 → ✅

**문제**

테스트 계정(`test01 / 1234`)이 운영 환경에서도 자동으로 생성될 수 있었다.

**수정 내용**

`@Profile("local")` 을 추가하여 `local` 프로파일에서만 시딩이 실행되도록 제한하였다.
운영(`prod`) 환경에서는 DataInitializer 자체가 Bean으로 등록되지 않는다.

```java
// 수정 후 — DataInitializer.java
@Component
@Profile("local")   // 추가
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner { ... }
```

**수정 파일**

| 파일 | 변경 내용 |
|------|----------|
| `config/DataInitializer.java` | `@Profile("local")` 추가, 비밀번호 해싱 적용 |

---

## 4. 미수정 항목 및 향후 개선 방향

### 4-1. API 인증/인가 없음 🔴 (향후 개선 필요)

현재 로그인 후 토큰이나 세션 검증 없이 URL만 알면 타인의 데이터에 접근 가능하다.

```
GET /api/screening/users/{userId}/profile       → 누구나 조회 가능
GET /api/screening/applications/my/{userId}     → 누구나 조회 가능
POST /api/screening/applications/{id}/screening → 누구나 실행 가능
```

**개선 방향**: JWT 토큰 인증 도입 또는 Spring Security 필터 체인 구성

---

### 4-2. sessionStorage에 사용자 정보 저장 🟡 (향후 개선 필요)

프론트엔드에서 로그인 후 `userId`를 `sessionStorage`에 저장하고 있어
XSS 취약점이 존재할 경우 스크립트로 읽힐 수 있다.

```ts
// Login.tsx
sessionStorage.setItem('userId', res.data.userId)
```

**개선 방향**: 서버 사이드 세션 또는 HTTP-only 쿠키 기반 인증으로 전환

---

### 4-3. H2 콘솔 및 SQL 로그 🟢 (운영 환경 설정 주의)

`application-local.yml` 에만 설정되어 있으므로 로컬에서는 문제없다.
운영 profile(`application-prod.yml`) 작성 시 반드시 아래와 같이 설정한다.

```yaml
# application-prod.yml
spring:
  h2:
    console:
      enabled: false
  jpa:
    show-sql: false
```

---

## 5. 수정 후 검증 결과

| 항목 | 명령 | 결과 |
|------|------|------|
| Java 컴파일 | `./gradlew compileJava` | ✅ BUILD SUCCESSFUL |
| Spring Boot 통합 테스트 | `./gradlew test` | ✅ BUILD SUCCESSFUL |

---
