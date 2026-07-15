"""정규식 기반 필드 추출 유틸리티 (이름/이메일/전화/날짜/기업명).

여기서 정의된 함수들은 순수 텍스트 → 값 변환만 담당한다. confidence 산정과
ExtractedField로의 포장은 rule_based_parser에서 수행한다.
"""
import re

EMAIL_EXTRACT_RE = re.compile(r"[\w.+-]+@(?:[\w-]+\.)+[a-zA-Z]{2,}")
PHONE_EXTRACT_RE = re.compile(r"(?:\+?82[-\s]?)?0\d{1,2}[-\s]?\d{3,4}[-\s]?\d{4}")

NAME_LABEL_RE = re.compile(r"^(?:성명|이름|Name)\s*[:：]\s*(.+)$", re.IGNORECASE)

# YYYY.MM / YYYY-MM / YYYY년 M월 (일자는 무시)
DATE_TOKEN_RE = re.compile(r"(\d{4})\s*[.\-년]\s*(\d{1,2})\s*월?")

CURRENT_KEYWORDS = ["현재", "재직중", "재직 중", "present"]

COMPANY_SUFFIX_RE = re.compile(
    r"(주식회사|㈜|\(주\)|유한회사|Inc\.?|Co\.,?\s*Ltd\.?|Corporation|Corp\.?)",
    re.IGNORECASE,
)

TEST_NAME_LANGUAGE_MAP = {
    "TOEIC": "영어",
    "TOEFL": "영어",
    "IELTS": "영어",
    "OPIC": "영어",
    "OPIC(영어)": "영어",
    "TEPS": "영어",
    "JLPT": "일본어",
    "JPT": "일본어",
    "HSK": "중국어",
}


def extract_emails(text: str) -> list[str]:
    return EMAIL_EXTRACT_RE.findall(text)


def extract_phones(text: str) -> list[str]:
    return PHONE_EXTRACT_RE.findall(text)


def extract_name_from_label(text: str) -> str | None:
    """"성명: 홍길동" / "이름: 홍길동" / "Name: Hong Gildong" 형태에서 값 추출."""
    match = NAME_LABEL_RE.match(text.strip())
    if match:
        return match.group(1).strip()
    return None


def normalize_date_token(match: re.Match) -> str:
    year, month = match.group(1), match.group(2)
    return f"{year}-{int(month):02d}"


def extract_date_range(text: str) -> tuple[str | None, str | None, bool] | None:
    """텍스트에서 (start_date, end_date, is_current)를 추출한다.

    - 날짜 토큰이 2개 이상이면 (start, end, False)
    - 날짜 토큰이 1개이고 "현재/재직중/Present" 등이 함께 있으면 (start, None, True)
    - 날짜 토큰이 1개뿐이면 (start, None, False) — 단일 취득일 등에 사용
    - 날짜 토큰이 없으면 None
    """
    matches = list(DATE_TOKEN_RE.finditer(text))
    if not matches:
        return None

    is_current = any(kw in text.lower() for kw in CURRENT_KEYWORDS)

    start = normalize_date_token(matches[0])
    if len(matches) >= 2:
        end = normalize_date_token(matches[1])
        return (start, end, False)

    if is_current:
        return (start, None, True)

    return (start, None, False)


def extract_single_date(text: str) -> str | None:
    match = DATE_TOKEN_RE.search(text)
    if match:
        return normalize_date_token(match)
    return None


def contains_company_suffix(text: str) -> bool:
    return bool(COMPANY_SUFFIX_RE.search(text))


def infer_language_from_test_name(test_name: str | None) -> str | None:
    if not test_name:
        return None
    return TEST_NAME_LANGUAGE_MAP.get(test_name.strip().upper())
