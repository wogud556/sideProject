# 구현 계획 (planner 산출물)

> 주의: planner가 "원 명세 6장 필드 부재"로 educations/projects/certificates/
> skills/languages 필드를 추정했으나, 이후 SPEC.md 2장에 **정확한 필드 목록이
> 보강**되었다. 스키마 구현 시 SPEC.md의 필드 목록이 이 문서의 추정 필드보다
> 우선한다. 그 외 planner의 해석(아래 가정 2~4)은 그대로 채택한다.

## 채택된 가정
- basic 필드는 각각 `ExtractedField{value, confidence, source_page, source_text, review_required}`로 감싼다(필드 단위). careers 등 목록 항목은 레코드 단위로 confidence/source_page/source_text/review_required 1세트.
- 회원가입 화면 방식 채택 (별도 계정 생성 커맨드 없음).
- careers/educations는 전용 추가/삭제 라우트, 나머지 탭은 PATCH profile로 편집.

## 디렉터리 구조 (최종)

```
resume_intelligence/
├── manage.py
├── requirements.txt
├── .env.example
├── .gitignore
├── README.md
├── config/
│   ├── __init__.py  settings.py  urls.py  wsgi.py  test_runner.py
├── storage/
│   ├── __init__.py  json_store.py
├── apps/
│   ├── __init__.py
│   ├── accounts/
│   │   ├── __init__.py  apps.py  repository.py  services.py
│   │   ├── decorators.py  middleware.py  forms.py  views.py  urls.py
│   │   └── context_processors.py
│   ├── pdf_analysis/
│   │   ├── __init__.py  apps.py  pipeline.py
│   │   ├── schemas/  {__init__.py, common.py, profile.py, document.py}
│   │   ├── validators/  {__init__.py, file_validators.py, field_validators.py}
│   │   ├── extractors/  {__init__.py, base.py, pymupdf_extractor.py, quality.py, ocr_extractor.py, hybrid_extractor.py}
│   │   └── parsers/  {__init__.py, base.py, section_detector.py, field_extractors.py, rule_based_parser.py, llm_parser.py}
│   ├── resumes/
│   │   ├── __init__.py  apps.py  repository.py  forms.py  views.py  urls.py  permissions.py
│   │   ├── services/  {__init__.py, upload_service.py, careers_service.py, excel_export.py}
│   │   └── api/  {__init__.py, urls.py, views.py}
│   └── dart/
│       ├── __init__.py  apps.py  normalization.py  matching.py  account_mapping.py
│       ├── constants.py  repository.py  exceptions.py  views.py  urls.py
│       ├── clients/  {__init__.py, base.py, open_dart_client.py, dto.py}
│       ├── services/  {__init__.py, sync_service.py, company_service.py, linking_service.py}
│       ├── api/  {__init__.py, urls.py, views.py}
│       └── management/commands/sync_dart_corporations.py
├── templates/
│   ├── base.html
│   ├── accounts/  {login.html, signup.html}
│   ├── resumes/   {dashboard.html, upload.html, detail.html}
│   └── dart/      {company_search.html, company_detail.html}
├── static/css/base.css
├── data/            (gitignore, 런타임 생성)
└── tests/
    ├── __init__.py  helpers.py
    ├── fixtures/ {dart_company_response.json, dart_financials_response.json, dart_corp_list_sample.xml}
    └── test_*.py (아래 Phase별 참조)
```

## Phase 0 — 무DB Django 스켈레톤
- manage.py, config/{settings,urls,wsgi}.py, requirements.txt, .env.example, .gitignore, templates/base.html, static/css/base.css
- settings.py 핵심:
  - `DATABASES = {}` (DB 미사용 — runserver의 check_migrations는 조용히 스킵됨)
  - INSTALLED_APPS: staticfiles, messages, apps.accounts, apps.pdf_analysis, apps.resumes, apps.dart — **contrib.auth/admin/contenttypes/sessions 앱 금지**
  - MIDDLEWARE: Security, SessionMiddleware(앱 등록 없이 엔진만 사용), Common, Csrf, apps.accounts.middleware.CurrentUserMiddleware, Messages, XFrameOptions — **AuthenticationMiddleware 금지** (DB 조회함)
  - `SESSION_ENGINE = "django.contrib.sessions.backends.signed_cookies"`
  - `DATA_DIR = BASE_DIR / "data"` — 호출 시점에 settings.DATA_DIR 참조(임포트 시 캐싱 금지, 테스트에서 override_settings로 주입)
  - DATA_UPLOAD_MAX_MEMORY_SIZE / FILE_UPLOAD_MAX_MEMORY_SIZE = 25MB (사용자 친화적 20MB 검사는 validators에서)
  - `TEST_RUNNER = "config.test_runner.NoDatabaseTestRunner"`
- 검증: `python manage.py check` 클린 → runserver 기동

## Phase 1 — storage/json_store.py
```python
def read_json(path, default=None) -> Any
def write_json(path, data) -> None   # 부모 디렉터리 생성 → 임시파일 write → os.replace (원자적)
def update_json(path, mutate_fn, default) -> Any  # 파일 경로별 threading.Lock으로 read-modify-write 보호
```
- config/test_runner.py: DiscoverRunner의 setup_databases/teardown_databases를 no-op으로 오버라이드한 NoDatabaseTestRunner (안전망). 모든 테스트는 SimpleTestCase만 사용 — **django.test.TestCase 금지**.
- tests/test_storage.py

## Phase 2 — apps/accounts (JSON 인증)
- repository.py: list_users / get_user_by_username / get_user_by_id / create_user
- services.py: signup(중복검사+make_password), authenticate(check_password), login(request.session["user_id"]=...), logout, get_current_user
- decorators.py: login_required(템플릿→로그인 리다이렉트), api_login_required(JsonResponse 401)
- middleware.py: CurrentUserMiddleware → request.current_user 주입
- 회원가입/로그인/로그아웃 화면. tests/test_accounts.py

## Phase 3 — apps/pdf_analysis (순수 라이브러리, 파일 저장은 안 함)
- schemas: ExtractedBlock{page,order,text,bbox,method("text"|"ocr")}, ExtractedDocument{file_path,page_count,blocks,ocr_used,warnings}, PageQuality{page,char_count,broken_char_ratio,image_area_ratio,needs_ocr}, ExtractedField[T]{value,confidence,source_page,source_text,review_required}, AnalysisStatus enum, DocumentRecord, ResumeProfile(SPEC.md 2장 필드)
- validators/file_validators.py: validate_uploaded_pdf(확장자·MIME·%PDF- 시그니처·20MB·30페이지·암호화·빈 PDF)
- validators/field_validators.py: 이메일/전화/날짜 형식, 입사일≤퇴사일, 재직중, 중복 기간 경고
- extractors: PdfExtractor Protocol / PyMuPdfExtractor / quality.assess_page_quality / OcrExtractor(is_available()가 pytesseract.get_tesseract_version() try/except 1회 캐시, extract_page) / HybridExtractor(기본: 전체 추출→품질평가→낮은 페이지만 OCR→병합, 미설치 시 warnings에 OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED)
- parsers: ResumeParser Protocol / section_detector(기본정보/경력/학력/프로젝트/자격증/기술/어학 헤더 탐지) / field_extractors(이름·이메일·전화·날짜·기업명 정규식) / RuleBasedResumeParser(기본) / LlmResumeParser(NotImplementedError 스텁)
- pipeline.run_pipeline(file_path, extractor=None, parser=None) -> PipelineResult{blocks, profile, ocr_used, warnings, status}
- tests/helpers.py: PyMuPDF로 합성 PDF 생성(텍스트 PDF + 이미지 전용 페이지 PDF) — 실제 스캔본 없이 OCR 분기 테스트
- tests: test_pdf_validators, test_pdf_extractors, test_pdf_quality, test_ocr_graceful_degrade, test_section_detector, test_field_extractors, test_rule_based_parser, test_pipeline_integration

## Phase 4 — apps/resumes
- services/upload_service.upload_resume(user_id, uploaded_file) -> resume_id:
  검증 → uuid4 → original.pdf 저장 → document.json(UPLOADED) → run_pipeline 동기 호출(EXTRACTING/OCR_PROCESSING/STRUCTURING 상태 갱신) → blocks.json/profile.json 저장 → 최종 상태. 예외 시 부분 성공이면 COMPLETED_WITH_WARNINGS 우선.
- reanalyze_resume(resume_id, user_id, overwrite_user_edits=False)
- careers_service(경력/학력 추가·수정·삭제), excel_export(openpyxl)
- permissions.py: 소유자 검사(비소유자 404). PDF는 소유자 검사 뷰 경유로만 서빙.
- 화면: dashboard/upload/detail(탭: 기본정보/경력/학력/프로젝트/자격증/기술/원본 텍스트)
- api/: SPEC.md 5장 엔드포인트 전부 (Django JSON 뷰)
- tests: test_resume_upload_view, test_resume_editing, test_resume_api, test_excel_export, test_permissions

## Phase 5 — apps/dart
- normalization.normalize_company_name / matching.match_company(정규화 완전일치→정식명→부분 문자열→difflib 유사도; 완전일치 1건 MATCHED_AUTO, 복수 MULTIPLE_CANDIDATES, 없음 NOT_FOUND, 정규화명 2자 이하 자동연결 금지) / account_mapping
- clients/open_dart_client.OpenDartClient: is_configured / download_corp_codes(ZIP+XML) / get_company / get_financial_accounts. status 000/013/010/020/800/900 → DartNotFound/DartApiKeyMissing/DartRateLimited/DartApiError/DartTimeout 예외 매핑. Timeout 설정.
- services: sync_service(corporations.json 적재), company_service(캐시 우선 get_company_profile, get_financial_accounts CFS→OFS 폴백), linking_service(link_career_to_company — careers_service 경유로 profile.json 갱신)
- management command sync_dart_corporations
- 화면: company_search(경력별 후보/검색/선택/해제), company_detail(개황+재무 6계정+안내 문구)
- resumes 앱은 dart를 import하지 않음(독립). corp_code는 ^[0-9]{8}$ 검증 후에만 파일 경로 조합.
- tests: mock 기반 성공/013/010/020/Timeout, CFS→OFS 폴백, 정규화/매칭/계정매핑, sync command, DART_API_KEY 미설정 시 안내 응답 + 이력서 플로우 무영향
- fixtures: dart_company_response.json, dart_financials_response.json, dart_corp_list_sample.xml

## Phase 6 — 통합 배선
- config/urls.py 연결, base.html 네비, 루트 / 리다이렉트(로그인→대시보드)

## Phase 7 — 보안/로깅 점검
- PDF 직접 경로 비노출, UUID url 컨버터, corp_code 정규식, sha256 해시 중복 탐지, 로그에 개인정보/키 미기록. test_permissions에 비소유자/경로조작 케이스.

## Phase 8 — README + 완료 기준 확인
- README: 개요/스택/아키텍처/실행/환경변수/DART 동기화/테스트/샘플 PDF/제한사항
- SPEC.md 11장 완료 기준 체크리스트 확인, `python manage.py test` 전체 통과

## requirements.txt
```
Django>=5.1,<6.0
pydantic>=2.6,<3.0
PyMuPDF>=1.24,<2.0
pytesseract>=0.3.10,<0.4.0
Pillow>=10.0,<11.0
requests>=2.31,<3.0
openpyxl>=3.1,<4.0
python-dotenv>=1.0,<2.0
```
(DRF/Celery/psycopg 미포함. 테스트는 manage.py test + unittest.mock만.)

## 위험/컨벤션 요약
1. 테스트는 전부 SimpleTestCase (TestCase 금지 — DB 트랜잭션 래핑).
2. django.contrib.auth의 login_required/request.user 사용 금지 — apps.accounts 것만.
3. 동기 처리라 큰 스캔 PDF는 느릴 수 있음 → README에 한계 명시. 20MB/30페이지 사전 차단.
4. OCR graceful degrade (is_available 캐시).
5. DART 미설정/장애가 이력서 기능에 전파 금지.
6. update_json은 경로별 Lock + os.replace 원자적 쓰기.
7. repository는 dict in/out, service에서만 Pydantic model_validate/model_dump(mode="json").
