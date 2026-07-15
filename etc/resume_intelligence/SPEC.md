# Resume Intelligence Prototype — 조정 명세서 (1차)

PDF 이력서를 업로드하면 텍스트를 추출·항목별로 구조화하고, 경력의 기업명을
OpenDART 기업정보와 매칭해 기업개황·주요 재무정보를 조회하는 웹 애플리케이션.

## 0. 원 명세 대비 확정된 조정사항 (사용자 결정 — 최우선 적용)

| 항목 | 원 명세 | 확정 |
|---|---|---|
| 데이터 저장 | PostgreSQL | **DB 전혀 사용 안 함. 모든 데이터를 `data/` 폴더의 JSON 파일로 저장** (로그인 계정 포함) |
| 비동기 처리 | Celery + Redis | **동기 처리** (업로드 요청 안에서 분석까지 수행) |
| 구조화 | LLM + 규칙 | **규칙 기반 파서 기본. LLM 공급자 인터페이스(Protocol)만 정의, 실제 호출 구현은 스텁** |
| 프론트엔드 | Django Template + HTMX | 유지 (단, 동기 처리이므로 폴링 상태 화면은 단순화 가능) |
| Docker | Compose 필수 | 선택사항. `pip install` + `runserver`만으로 실행 가능해야 함 |

사용자는 데이터베이스를 전혀 모르는 사람이다. 설치·실행에 DB/Redis 등 외부
서버가 하나도 필요 없어야 하며, 저장된 데이터는 사람이 직접 열어볼 수 있는
JSON 파일이어야 한다. Django ORM/admin/contrib.auth 모델은 사용하지 않는다.

## 1. 기술 구성

- Python 3.12+, Django (템플릿 + 뷰, DB 미사용)
- 세션: `signed_cookies` 백엔드 (DB 불필요)
- 인증: `data/users.json` + `django.contrib.auth.hashers` 비밀번호 해시
- PDF: PyMuPDF(fitz). OCR: pytesseract + Pillow — **Tesseract 미설치 시 OCR을
  건너뛰고 경고만 남기는 graceful degrade** (설치 강제 금지)
- 구조화: 정규식/패턴 규칙 기반 파서, Pydantic 스키마 검증
- DART: requests 기반 OpenDART 클라이언트 (`DART_API_KEY` 환경변수, 없으면
  기능 비활성 안내), Timeout/오류 매핑, 테스트는 HTTP Mock
- Excel 내보내기: openpyxl (이력서 분석 결과 → .xlsx 다운로드 1개 엔드포인트)

## 2. 데이터 저장 구조 (JSON 파일 저장소)

```
data/
├── users.json                      # [{id, username, email, password_hash, created_at}]
├── resumes/
│   └── {resume_id}/
│       ├── document.json           # 업로드 메타 + 분석 상태
│       ├── original.pdf            # 원본 (UUID 기반 resume_id 디렉터리로 격리)
│       ├── blocks.json             # 페이지별 텍스트 블록 (page, order, text, bbox, method)
│       └── profile.json            # 구조화 결과 (기본정보/경력/학력/...)
└── dart/
    ├── corporations.json           # DART 고유번호 목록 (동기화 결과)
    ├── profiles/{corp_code}.json   # 기업개황 캐시
    └── financials/{corp_code}_{year}_{report}.json
```

- 저장소 계층: `storage/json_store.py` — 읽기/쓰기(원자적 쓰기: temp 파일 후
  rename), 잠금은 프로토타입 수준(단일 프로세스 가정)으로 단순화.
- resume_id는 UUID4. 파일명은 원본명 그대로 저장하지 않음.

### document.json 필드
id, user_id, original_filename, file_size, file_hash, page_count, is_encrypted,
analysis_status(UPLOADED/EXTRACTING/OCR_PROCESSING/STRUCTURING/COMPLETED/
COMPLETED_WITH_WARNINGS/FAILED), ocr_used, uploaded_at, analysis_completed_at,
error_code, error_message, warnings[]

### profile.json 구조 (각 추출값에 confidence, source_page, source_text, review_required 동반)
- basic: name, name_en, birth_date, email, phone, address, portfolio_url,
  github_url, linkedin_url, summary  (주민번호·사진·민감항목 저장 금지)
- careers[]: company_name_raw, company_name_normalized, department, position,
  employment_type, start_date(YYYY-MM), end_date|null, is_current,
  responsibilities[], achievements[], sort_order, confidence, review_required,
  dart_corp_code|null, dart_match_status(NOT_SEARCHED/MATCHED_AUTO/
  MATCHED_MANUAL/MULTIPLE_CANDIDATES/NOT_FOUND/ERROR)
- educations[]: school_name, major, degree, start_date, end_date,
  graduation_status, description, sort_order, confidence, review_required
- projects[]: project_name, organization, start_date, end_date, role,
  description, technologies[], achievements[], sort_order, confidence
- certificates[]: certificate_name, issuer, acquired_date, certificate_number, confidence
- skills[]: category, name, level, description, confidence
- languages[]: language, test_name, score, grade, acquired_date, confidence
- is_user_confirmed, confirmed_at

## 3. 처리 흐름 (동기)

업로드 → 검증(확장자·MIME·PDF 시그니처·크기 20MB·30페이지·암호화 여부·빈 PDF)
→ 저장 → 페이지별 텍스트 추출(PyMuPDF 블록+좌표) → 페이지 품질 평가(글자 수,
깨진 문자 비율, 이미지 면적) → 품질 낮은 페이지만 OCR(가능한 경우) → 섹션
탐지(기본정보/경력/학력/프로젝트/자격증/기술/어학) → 규칙 기반 필드 추출 →
Pydantic 검증 → 검증 규칙(이메일/전화/날짜 형식, 입사일≤퇴사일, 재직중 처리,
중복 기간 경고) → profile.json 저장 → 상세 화면으로 이동.

부분 실패 시 FAILED가 아닌 COMPLETED_WITH_WARNINGS + warnings[] 사용.

## 4. 화면 (Django Template + Bootstrap, HTMX 선택)

1. 로그인/로그아웃 (+ 최초 실행용 계정 생성 커맨드 또는 회원가입 화면 중 택1)
2. 대시보드: 전체/완료/실패/매칭필요 건수, 최근 업로드 목록
3. PDF 업로드: 파일 선택, 제약 안내, 개인정보 안내
4. 이력서 상세·수정: 탭(기본정보/경력/학력/프로젝트/자격증/기술/원본 텍스트),
   필드 수정, 경력·학력 추가/삭제, 신뢰도·검토필요 표시, 확인 완료(CONFIRMED),
   재분석(사용자 수정값 덮어쓰지 않음 기본), Excel 다운로드. PDF 원본 다운로드/
   미리보기는 소유자 검사 후 뷰를 통해 서빙(직접 경로 노출 금지).
5. 기업 매칭: 경력별 원본/정규화 기업명, 매칭 상태, 후보 목록(회사명·종목코드·
   유사도), 검색·선택·연결 해제
6. 기업 상세: 개황 + 최근 사업연도 주요 재무 6계정(자산/부채/자본/매출/영업이익/
   당기순이익) + 출처 안내 문구

## 5. API (Django JSON 뷰, `/api/v1/`) — 화면과 동일 서비스 계층 사용

POST /resumes (업로드) · GET /resumes · GET /resumes/{id} ·
GET /resumes/{id}/status · PATCH /resumes/{id}/profile ·
POST|PATCH|DELETE /resumes/{id}/careers[/{cid}] · POST /resumes/{id}/confirm ·
POST /resumes/{id}/reanalyze · GET /dart/corporations/search?keyword= ·
POST /resumes/{id}/careers/{cid}/dart-company · GET /dart/corporations/{corp_code} ·
GET /dart/corporations/{corp_code}/financials?year=&report_code= ·
POST /dart/corporations/{corp_code}/refresh

모두 로그인 필수 + 본인 소유 이력서만 접근 가능.

## 6. DART 연계

- 고유번호 ZIP/XML 다운로드 → corporations.json 적재. 실행:
  `python manage.py sync_dart_corporations` (management command, DB 없이 동작)
- 기업명 정규화: 주식회사/㈜/(주)/유한회사 제거, 공백·하이픈·특수문자 제거,
  영문 소문자화. 원본명 별도 보존.
- 매칭 순서: 정규화 완전일치 → 정식명 완전일치 → 부분 문자열 → 유사도
  (difflib). 완전일치 1건이면 자동 연결 후보(MATCHED_AUTO), 복수면
  MULTIPLE_CANDIDATES로 사용자 선택, 없으면 NOT_FOUND. 짧은 이름(2자 이하)은
  자동 연결 금지.
- 기업개황 API(company.json), 단일회사 주요계정 API(fnlttSinglAcnt.json):
  CFS 우선, 없으면 OFS. 계정명 매핑 테이블(매출액/영업수익/수익(매출액)...,
  영업이익/영업이익(손실)...) + 원본 계정명 함께 저장.
- DART 오류(키 없음/한도 초과/미검색/장애/Timeout)가 이력서 기능에 영향을
  주지 않아야 함.

## 7. 모듈 인터페이스 (Protocol)

- `PdfExtractor.extract(file_path) -> ExtractedDocument`
  (PyMuPdfExtractor / OcrExtractor / HybridExtractor — Hybrid가 기본)
- `ResumeParser.parse(document) -> ResumeParseResult`
  (RuleBasedResumeParser 기본. `LlmResumeParser`는 인터페이스 + NotImplemented
  스텁, 공급자 SDK 미결합)
- `DartClient.get_company(corp_code)` / `get_financial_accounts(corp_code,
  business_year, report_code, fs_div)` — DTO/매퍼 사용, 응답 원본은 캐시 파일에 보존

## 8. 보안

- 로그인 필수, 본인 이력서만 접근, PDF 직접 경로 비노출
- 확장자+MIME+PDF 시그니처(%PDF-) 검사, UUID 저장 경로, 해시 저장(중복 탐지)
- 로그에 이력서 원문/전화/이메일/주소/생년월일/API 키 기록 금지
- 환경변수: DJANGO_SECRET_KEY, DART_API_KEY (.env.example 제공, .env 커밋 금지)

## 9. 디렉터리 구조

```
resume_intelligence/
├── manage.py
├── requirements.txt  (또는 pyproject.toml)
├── .env.example
├── README.md
├── config/ (settings.py — DB 미설정, urls.py, wsgi.py)
├── apps/
│   ├── accounts/      # JSON 사용자 저장소 + 로그인 뷰 + 데코레이터
│   ├── resumes/       # 업로드/상세/수정 뷰, services/, api/
│   ├── pdf_analysis/  # extractors/, parsers/, schemas/, validators/
│   └── dart/          # clients/, services/, management/commands/
├── storage/           # json_store.py (원자적 읽기/쓰기)
├── templates/  static/  data/(gitignore)  tests/
```

비즈니스 로직은 뷰가 아닌 서비스 계층에 배치.

## 10. 테스트

- 단위: 파일 검증(비PDF/암호화/크기·페이지 초과), 텍스트 추출, OCR 판별,
  이름/이메일/전화/경력/날짜 정규화/재직중 추출, null 처리, 기업명 정규화,
  완전일치/다중후보/미검색 매칭, DART Mock(성공/없음/인증오류/한도/Timeout),
  CFS→OFS 폴백, 계정 매핑, JSON 저장소 원자성
- 통합: 업로드→분석→profile.json 생성→수정→확인, DART 검색→연결→개황→재무
- 실제 DART API 호출 금지 (전부 Mock/Fixture)

## 11. 완료 기준

1. `pip install -r requirements.txt` + `python manage.py runserver` 만으로 실행
   (DB·Redis·Docker 불필요)
2. 로그인 → PDF 업로드 → 분석 → 상세 화면 수정 → 확인 완료 동작
3. 텍스트 PDF/스캔 PDF 구분, OCR 가능 시 적용·불가 시 안내
4. data/ 폴더의 JSON 파일로 모든 데이터 확인 가능, Excel 다운로드 동작
5. DART 키 설정 시 동기화→검색→선택→개황→재무 조회 동작, 실패해도 이력서 기능 정상
6. 핵심 단위·통합 테스트 통과, README만 보고 실행 가능
