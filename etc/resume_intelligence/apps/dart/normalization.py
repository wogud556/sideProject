"""기업명 정규화 (SPEC.md 6장).

주식회사/㈜/(주)/유한회사 제거, 공백·하이픈·특수문자 제거, 영문 소문자화.
원본명은 별도로 보존한다(이 함수는 정규화된 문자열만 반환).
"""
import re

_COMPANY_SUFFIX_RE = re.compile(r"주식회사|㈜|\(주\)|유한회사")
_STRIP_CHARS_RE = re.compile(r"[\s\-·.,()\[\]{}'\"]+")


def normalize_company_name(name: str | None) -> str:
    if not name:
        return ""
    normalized = _COMPANY_SUFFIX_RE.sub("", name)
    normalized = _STRIP_CHARS_RE.sub("", normalized)
    return normalized.lower()
