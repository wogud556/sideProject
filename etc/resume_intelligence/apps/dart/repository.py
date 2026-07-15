"""data/dart/ 기반 저장소 (고유번호 목록/기업개황 캐시/재무정보 캐시)."""
import re
from pathlib import Path
from typing import Any

from django.conf import settings

from storage.json_store import read_json, write_json

from . import constants

_CORP_CODE_RE = re.compile(constants.CORP_CODE_PATTERN)


def _validate_corp_code(corp_code: str) -> None:
    """corp_code로 파일 경로를 조합하기 전 형식을 재검증한다(defense in depth).

    호출부(views/api)에서 이미 검증하지만, 이 함수는 경로 조합 지점에서
    한 번 더 확인해 향후 새 호출 경로가 검증을 빠뜨리더라도 경로 조작으로
    이어지지 않도록 한다.
    """
    if not _CORP_CODE_RE.match(corp_code):
        raise ValueError(f"올바르지 않은 corp_code입니다: {corp_code!r}")


def dart_root() -> Path:
    return settings.DATA_DIR / "dart"


def corporations_path() -> Path:
    return dart_root() / "corporations.json"


def profile_path(corp_code: str) -> Path:
    _validate_corp_code(corp_code)
    return dart_root() / "profiles" / f"{corp_code}.json"


def financials_path(corp_code: str, business_year: str, report_code: str) -> Path:
    _validate_corp_code(corp_code)
    return dart_root() / "financials" / f"{corp_code}_{business_year}_{report_code}.json"


def read_corporations() -> list[dict[str, Any]]:
    return read_json(corporations_path(), default=[])


def write_corporations(data: list[dict[str, Any]]) -> None:
    write_json(corporations_path(), data)


def read_company_profile(corp_code: str) -> dict[str, Any] | None:
    return read_json(profile_path(corp_code))


def write_company_profile(corp_code: str, data: dict[str, Any]) -> None:
    write_json(profile_path(corp_code), data)


def read_financials(corp_code: str, business_year: str, report_code: str) -> dict[str, Any] | None:
    return read_json(financials_path(corp_code, business_year, report_code))


def write_financials(corp_code: str, business_year: str, report_code: str, data: dict[str, Any]) -> None:
    write_json(financials_path(corp_code, business_year, report_code), data)
