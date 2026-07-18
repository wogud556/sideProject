# Resume Intelligence

PDF 이력서를 업로드하면 텍스트를 추출·항목별로 구조화하고, 경력의 기업명을
OpenDART 기업정보와 매칭해 기업개황·주요 재무정보를 조회하는 웹 애플리케이션.

DB(PostgreSQL 등)와 Redis, Docker 없이 **`pip install` + `runserver`만으로**
실행할 수 있도록 설계되었다. 모든 데이터(계정 포함)는 `data/` 폴더 아래
JSON 파일로 저장되며, 사람이 직접 텍스트 에디터로 열어볼 수 있다.

## 1. 개요

- 로그인한 사용자가 PDF 이력서를 업로드하면, 업로드 요청 안에서 동기적으로
  텍스트 추출 → (필요 시) OCR → 섹션 탐지 → 규칙 기반 필드 추출 →
  검증까지 수행하고 결과를 보여준다.
- 상세 화면에서 추출된 값(신뢰도·검토필요 표시)을 확인/수정하고, 경력의
  회사명을 OpenDART 기업 데이터와 매칭해 기업개황·재무정보를 조회할 수
  있다.
- 분석 결과는 Excel(.xlsx)로 내보낼 수 있다.

## 2. 기술 스택

- Python 3.12+, Django 5.x (템플릿 + 뷰, **DB 미사용**)
- 세션: `django.contrib.sessions.backends.signed_cookies` (DB 불필요)
- 인증: `data/users.json` + `django.contrib.auth.hashers` 비밀번호 해시
  (Django ORM/`contrib.auth` 모델은 사용하지 않음)
- PDF 추출: PyMuPDF(fitz) / OCR: pytesseract + Pillow (Tesseract 미설치 시
  자동으로 건너뛰고 경고만 남김)
- 구조화: 정규식 기반 규칙 파서 + Pydantic 스키마 검증 (LLM 파서는 인터페이스만
  정의된 스텁)
- DART 연계: `requests` 기반 OpenDART 클라이언트
- Excel 내보내기: openpyxl
- 테스트: `django.test.SimpleTestCase` + `unittest.mock` (DB 트랜잭션에
  의존하는 `TestCase`는 사용하지 않음)

## 3. 아키텍처

```
resume_intelligence/
├── manage.py
├── requirements.txt
├── .env.example
├── config/            # settings.py(DATABASES={}), urls.py, wsgi.py, test_runner.py
├── storage/           # json_store.py — 원자적 읽기/쓰기(temp 파일 후 os.replace) + 경로별 Lock
├── apps/
│   ├── accounts/       # JSON 사용자 저장소 + 로그인/회원가입 뷰 + 데코레이터 + 미들웨어
│   ├── pdf_analysis/   # 순수 라이브러리: 추출기/파서/스키마/검증기 (스스로 저장하지 않음)
│   ├── resumes/        # 업로드/상세/수정 화면 + services/ + api/ (오케스트레이션 담당)
│   └── dart/           # DART 클라이언트/매칭/캐시 서비스 + management command + api/
├── templates/, static/
├── data/               # 런타임 생성, .gitignore 대상 (모든 저장 데이터)
└── tests/              # SimpleTestCase 기반 단위/통합 테스트 + fixtures/
```

- 비즈니스 로직은 각 앱의 `services/`(또는 최상위 서비스 모듈)에 있고, 뷰는
  요청 파싱과 서비스 호출·응답 변환만 담당하는 얇은 계층이다.
- `apps.pdf_analysis`는 순수 라이브러리로, 파일을 스스로 저장하지 않는다.
  실제 `data/resumes/{resume_id}/`에 파일을 쓰는 오케스트레이션은
  `apps.resumes.services.upload_service`가 담당한다.
- `apps.resumes`는 `apps.dart`를 import하지 않는다. 반대로 `apps.dart`의
  `linking_service`가 `apps.resumes`의 `careers_service`를 통해 경력의
  DART 연결 상태를 갱신한다(의존 방향은 dart → resumes 단방향).

### 데이터 저장 구조

```
data/
├── users.json
├── resumes/{resume_id}/{document.json, original.pdf, blocks.json, profile.json}
└── dart/
    ├── corporations.json
    ├── profiles/{corp_code}.json
    └── financials/{corp_code}_{year}_{report}.json
```

## 4. 설치 및 실행

**요구 사항: Python 3.12 이상.** (Django 5.x는 Python 3.10+ 를 요구하며,
개발 환경은 3.12로 검증했다. 시스템 기본 `python3`가 3.9 이하라면
`brew install python@3.12` 등으로 별도 설치 후 그 인터프리터로 가상환경을
만들어야 한다.)

```bash
cd resume_intelligence
python3.12 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt

cp .env.example .env               # 필요 시 DJANGO_SECRET_KEY / DART_API_KEY 채우기

python manage.py check             # DB 없이도 정상 통과해야 한다
python manage.py runserver
```

브라우저에서 `http://localhost:8000` 접속 → 회원가입 → 로그인 → 이력서
업로드 순서로 사용한다. **DB·Redis·Docker는 전혀 필요 없다.**

### OCR(Tesseract) — 선택 설치

스캔된(텍스트 레이어가 없는) PDF는 OCR이 필요하다. Tesseract가 설치되어
있지 않아도 애플리케이션은 정상 동작하며, 텍스트 추출이 불가능한 페이지에는
`OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED` 경고만 남기고 넘어간다
(graceful degrade). OCR을 사용하려면:

```bash
# macOS
brew install tesseract tesseract-lang   # 한국어(kor) 언어팩 포함

# Debian/Ubuntu
sudo apt-get install tesseract-ocr tesseract-ocr-kor
```

Windows는 UB Mannheim 설치 파일(https://github.com/UB-Mannheim/tesseract/wiki)을
실행하고, 설치 중 "Additional language data" 단계에서 **Korean**을 체크한 뒤,
시스템 환경변수 PATH에 `C:\Program Files\Tesseract-OCR`를 추가한다
(새 명령 창에서 `tesseract --version`이 출력되면 성공).

설치 여부는 앱 재시작 없이도 첫 OCR 시도 시 자동 감지된다(프로세스 내
1회 캐시).

### Windows에서 실행

코드는 수정 없이 Windows에서 동작한다 (모든 파일 IO가 UTF-8 명시,
`pathlib` 경로 사용). 단, macOS/Linux에서 만든 `.venv` 폴더는 해당 OS 전용
바이너리이므로 Windows에서는 가상환경을 새로 만들어야 한다.

1. python.org 에서 Python 3.12 이상 설치 — 설치 첫 화면에서
   **"Add python.exe to PATH" 반드시 체크**
2. 프로젝트를 복사할 때 `.venv/` 폴더는 제외 (git clone 권장)
3. 명령 프롬프트/PowerShell에서:

```bat
cd resume_intelligence
py -3.12 -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt

copy .env.example .env

python manage.py check
python manage.py runserver
```

이후 사용법(브라우저 접속, DART 동기화, 테스트 실행)은 위와 동일하다.

## 5. 환경변수 (`.env`)

| 변수 | 필수 | 설명 |
|---|---|---|
| `DJANGO_SECRET_KEY` | 운영 환경 권장 | 세션 서명 등에 사용. 미설정 시 개발용 기본값 사용 |
| `DJANGO_DEBUG` | 선택 | 기본 `True` |
| `DJANGO_ALLOWED_HOSTS` | 선택 | 콤마로 구분, 기본 `localhost,127.0.0.1` |
| `DART_API_KEY` | 선택 | OpenDART Open API 인증키. **미설정 시 기업 매칭/조회 화면만 안내 메시지를 보여주고, 이력서 업로드·분석·수정 등 나머지 기능은 정상 동작한다.** |

`.env`는 커밋하지 않는다(`.gitignore`에 포함). `.env.example`을 복사해서 사용한다.

## 6. API 인증/CSRF

`/api/v1/...` JSON 엔드포인트는 로그인 세션 쿠키로 인증하며, Django의
`CsrfViewMiddleware`가 그대로 적용된다(상태를 바꾸는 POST/PATCH/DELETE
요청은 CSRF 토큰이 반드시 필요하다). 브라우저에서 템플릿 화면으로만
사용한다면 각 폼의 `{% csrf_token %}`이 알아서 처리해주므로 신경 쓸 필요가
없다. 스크립트/외부 클라이언트에서 JSON API를 직접 호출하려면:

1. 먼저 로그인한다(`POST /accounts/login/` — 세션 쿠키 발급).
2. `GET /api/v1/csrf`를 호출한다. 응답 바디의 `csrfToken` 값과 동일한 값이
   `csrftoken` 쿠키에도 설정된다.
3. 이후 상태 변경 요청(POST/PATCH/DELETE)에 `X-CSRFToken` 헤더로 그 값을
   담아 보낸다(쿠키는 요청에 항상 자동으로 실려 있어야 한다).

```bash
# 세션 쿠키를 저장하며 로그인
curl -c cookies.txt -b cookies.txt \
  -d "csrfmiddlewaretoken=$(curl -s -c cookies.txt http://localhost:8000/accounts/login/ \
        | grep -o 'csrfmiddlewaretoken[^>]*value=\"[^\"]*\"' | sed 's/.*value=\"//;s/\"//')" \
  -d "username=alice" -d "password=your-password" \
  http://localhost:8000/accounts/login/

# CSRF 토큰 획득
TOKEN=$(curl -s -b cookies.txt -c cookies.txt http://localhost:8000/api/v1/csrf | python3 -c "import sys,json;print(json.load(sys.stdin)['csrfToken'])")

# 토큰을 헤더로 담아 상태 변경 요청
curl -b cookies.txt -c cookies.txt -X PATCH \
  -H "X-CSRFToken: $TOKEN" -H "Content-Type: application/json" \
  -d '{"basic": {"name": "새 이름"}}' \
  http://localhost:8000/api/v1/resumes/<resume_id>/profile
```

토큰 없이 상태 변경 요청을 보내면 `403 Forbidden`이 반환된다
(`tests/test_api_csrf.py`에서 검증).

## 7. DART 기업 데이터 동기화

기업 매칭 기능을 쓰려면 먼저 DART 고유번호 목록을 내려받아야 한다
(`DART_API_KEY` 필요):

```bash
python manage.py sync_dart_corporations
```

성공하면 `data/dart/corporations.json`에 전체 상장·비상장 기업 목록이
저장된다. 이후 이력서 상세 화면의 경력 항목에서 "기업 매칭"으로 들어가면
회사명을 정규화 완전일치 → 정식명 완전일치 → 부분 문자열 → 유사도(difflib)
순으로 자동/후보 매칭한다.

DART API가 응답하지 않거나(Timeout), 키가 없거나(010), 한도를
초과했거나(020) 등 어떤 이유로 실패하더라도 이력서 업로드·조회·수정 기능은
영향을 받지 않는다(`tests/test_dart_resume_independence.py`에서 검증).

## 8. 테스트 실행

```bash
python manage.py test
```

- 모든 테스트는 `django.test.SimpleTestCase`만 사용한다(DB 트랜잭션에
  의존하는 `TestCase`는 사용하지 않음). `TEST_RUNNER`도 DB 셋업/해제를
  no-op으로 오버라이드한 `config.test_runner.NoDatabaseTestRunner`로
  설정되어 있어 DB 없이 그대로 실행된다.
- 실제 OpenDART API는 절대 호출하지 않는다 — 전부 `unittest.mock` +
  `tests/fixtures/`의 응답 예시로 검증한다.
- PDF 관련 테스트는 실제 샘플 파일 없이 PyMuPDF로 합성 PDF(텍스트 이력서 /
  이미지 전용 스캔본 / 암호화 PDF / 빈 PDF)를 즉석에서 생성해 사용한다
  (`tests/helpers.py`).
- Tesseract가 설치된 환경에서는 OCR 경로가 실제 엔진으로 end-to-end
  검증되고, 설치되지 않은 환경에서는 해당 테스트만 자동으로 스킵되고
  나머지는 정상 통과한다.

특정 모듈만 실행하려면:

```bash
python manage.py test tests.test_pipeline_integration
python manage.py test tests.test_dart_client
```

## 9. 샘플 PDF로 직접 테스트해보기

리포지토리에 별도의 샘플 PDF 파일을 두지 않는다(개인정보 이슈 방지). 대신
아래처럼 파이썬 셸에서 테스트에 쓰이는 것과 동일한 헬퍼로 합성 이력서
PDF를 만들어 업로드해볼 수 있다.

```bash
python manage.py shell
```

```python
import sys
sys.path.insert(0, "tests")
from helpers import build_text_resume_pdf

with open("/tmp/sample_resume.pdf", "wb") as f:
    f.write(build_text_resume_pdf())
```

생성된 `/tmp/sample_resume.pdf`를 업로드 화면(`/resumes/upload/`)에서
선택하면 이름/이메일/전화/경력/학력/프로젝트/자격증/기술/어학이 채워진
상태로 분석이 완료된다. 스캔본(OCR) 동작을 보고 싶다면 `build_image_only_pdf(...)`
를 사용하면 된다.

## 10. 알려진 제한사항

- **동기 처리**: 업로드 요청 안에서 추출·OCR·구조화를 모두 수행하므로,
  페이지가 많거나 OCR이 필요한 스캔본은 응답이 느릴 수 있다. 20MB/30페이지
  제한으로 극단적인 지연은 사전 차단한다.
- **규칙 기반 파서의 한계**: 경력/학력/프로젝트는 "제목 줄 다음에 날짜범위
  줄이 오는" 2줄 패턴을 전제로 그룹핑한다. 표 형식이거나 날짜가 제목과 같은
  줄에 있는 등 다른 레이아웃의 이력서는 정확도가 떨어질 수 있다(이 경우
  confidence가 낮게 설정되고 `review_required=True`로 표시된다).
  자격증/어학 항목은 공백 기준으로 명칭/기관/시험명을 나누는 단순 휴리스틱을
  사용한다.
- **LLM 파서 미구현**: `apps.pdf_analysis.parsers.llm_parser.LlmResumeParser`는
  인터페이스만 정의되어 있고 호출 시 `NotImplementedError`를 발생시키는
  스텁이다.
- **OCR 언어팩**: `kor+eng`로 시도하고 실패 시 `eng`로만 재시도한다. 다른
  언어 이력서는 별도 언어팩 설치가 필요하다.
- **경력/학력 항목 식별자**: SPEC 필드 목록에 별도 id가 없어, API의
  `/careers/{index}`, `/educations/{index}`는 리스트 내 위치(0부터 시작하는
  정수 인덱스)를 식별자로 사용한다. 항목을 삭제하면 이후 인덱스가
  당겨진다.
- **단일 프로세스 가정**: `storage/json_store.py`의 잠금은
  `threading.Lock` 기반으로, 여러 프로세스(멀티 워커)에서 동시에 같은
  파일을 수정하는 상황까지는 보장하지 않는다(프로토타입 수준). 단일
  프로세스 내에서는 경로별 락으로 profile.json/document.json/users.json에
  대한 동시 read-modify-write가 직렬화된다(`tests/test_resume_concurrency.py`,
  `tests/test_accounts.py::ConcurrentSignupTests`에서 검증).
- **로컬 프로토타입 전제의 보안 기본값**: `DJANGO_SECRET_KEY`를 설정하지
  않으면 개발용 하드코딩된 기본값을 사용하고, `DJANGO_DEBUG`도 기본
  `True`다. 세션/CSRF 쿠키에도 `Secure` 플래그를 강제하지 않는다(로컬
  HTTP 실행을 전제). **운영 배포 시에는 반드시** `DJANGO_SECRET_KEY`를
  무작위 값으로 설정하고, `DJANGO_DEBUG=False`로 바꾸고, HTTPS 뒤에서
  `CSRF_COOKIE_SECURE`/`SESSION_COOKIE_SECURE`를 `True`로 켜야 한다(현재
  settings.py에는 이 두 설정이 명시적으로 추가되어 있지 않다).
- **목록 조회 성능**: `list_documents_for_user`, `find_by_hash` 등은
  `data/resumes/` 아래 모든 디렉터리를 순회하는 풀 스캔이다. 이력서
  개수가 매우 많아지면(수만 건 이상) 대시보드/업로드 응답이 느려질 수
  있다 — 별도의 사용자별 인덱스 파일이 없는 순수 JSON 파일 저장소의
  트레이드오프다.
- **경력/학력 식별자와 동시 편집**: 인덱스 기반 식별자는 삭제 시
  당겨지므로, 같은 이력서를 여러 탭/기기에서 동시에 편집하면 사용자가
  보고 있던 인덱스가 실제로는 다른 항목을 가리키게 될 수 있다(락은
  파일 손상만 방지하며, 이런 종류의 논리적 충돌까지 해결하지 않는다).
