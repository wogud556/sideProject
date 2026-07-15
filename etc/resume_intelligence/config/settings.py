"""
Django settings for resume_intelligence project.

이 프로젝트는 데이터베이스를 전혀 사용하지 않는다 (SPEC.md 0장 참고).
모든 데이터는 DATA_DIR 아래 JSON 파일로 저장한다.
"""
import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

load_dotenv(BASE_DIR / ".env")

SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "dev-insecure-secret-key-change-me")

DEBUG = os.environ.get("DJANGO_DEBUG", "True") == "True"

ALLOWED_HOSTS = os.environ.get("DJANGO_ALLOWED_HOSTS", "localhost,127.0.0.1").split(",")

# NOTE: contrib.auth / contrib.admin / contrib.contenttypes / contrib.sessions
# 는 등록하지 않는다 (DB 조회를 유발함). staticfiles/messages는 DB 불필요.
INSTALLED_APPS = [
    "django.contrib.staticfiles",
    "django.contrib.messages",
    "apps.accounts",
    "apps.pdf_analysis",
    "apps.resumes",
    "apps.dart",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    # contrib.sessions 앱은 등록하지 않지만 세션 엔진(signed_cookies)은 DB가
    # 필요 없으므로 미들웨어만 사용한다.
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    # django.contrib.auth.middleware.AuthenticationMiddleware는 DB를 조회하므로 금지.
    "apps.accounts.middleware.CurrentUserMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.messages.context_processors.messages",
                "apps.accounts.context_processors.current_user",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# 이 프로젝트는 데이터베이스를 전혀 사용하지 않는다.
DATABASES = {}

LANGUAGE_CODE = "ko-kr"
TIME_ZONE = "Asia/Seoul"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATICFILES_DIRS = [BASE_DIR / "static"]

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# 세션: DB 없이 서명된 쿠키에 저장.
SESSION_ENGINE = "django.contrib.sessions.backends.signed_cookies"

# 모든 데이터(JSON)의 루트 디렉터리. settings.DATA_DIR을 호출 시점에 참조할 것
# (임포트 시 캐싱 금지 — 테스트에서 override_settings로 임시 디렉터리 주입).
DATA_DIR = BASE_DIR / "data"

# 사용자 친화적인 20MB 검사는 apps.pdf_analysis.validators에서 수행한다.
DATA_UPLOAD_MAX_MEMORY_SIZE = 25 * 1024 * 1024
FILE_UPLOAD_MAX_MEMORY_SIZE = 25 * 1024 * 1024

TEST_RUNNER = "config.test_runner.NoDatabaseTestRunner"

DART_API_KEY = os.environ.get("DART_API_KEY", "")
