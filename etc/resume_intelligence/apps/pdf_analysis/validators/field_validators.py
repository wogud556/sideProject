"""필드 형식 검증 및 경력 날짜 규칙 검증 (SPEC.md 3장).

이메일/전화/날짜 형식 검증, 입사일≤퇴사일, 재직중 처리, 중복 기간 경고.
"""
import re

EMAIL_RE = re.compile(r"^[\w.+-]+@([\w-]+\.)+[a-zA-Z]{2,}$")
PHONE_RE = re.compile(r"^(\+?82-?)?0?1[016789]-?\d{3,4}-?\d{4}$|^(\+?82-?)?0\d{1,2}-?\d{3,4}-?\d{4}$")
DATE_YM_RE = re.compile(r"^\d{4}-(0[1-9]|1[0-2])$")


def is_valid_email(value: str | None) -> bool:
    if not value:
        return False
    return bool(EMAIL_RE.match(value.strip()))


def is_valid_phone(value: str | None) -> bool:
    if not value:
        return False
    return bool(PHONE_RE.match(value.strip()))


def is_valid_date_ym(value: str | None) -> bool:
    """YYYY-MM 형식(정규화된 날짜) 검증."""
    if not value:
        return False
    return bool(DATE_YM_RE.match(value.strip()))


def validate_career_period(start_date: str | None, end_date: str | None, is_current: bool) -> list[str]:
    """경력 하나의 기간 규칙을 검증하고 경고 메시지 목록을 반환한다."""
    warnings: list[str] = []

    if is_current and end_date:
        warnings.append("재직중으로 표시되었지만 종료일이 입력되어 있습니다.")

    if start_date and not is_valid_date_ym(start_date):
        warnings.append(f"입사일 형식이 올바르지 않습니다: {start_date}")

    if end_date and not is_valid_date_ym(end_date):
        warnings.append(f"퇴사일 형식이 올바르지 않습니다: {end_date}")

    if (
        start_date
        and end_date
        and is_valid_date_ym(start_date)
        and is_valid_date_ym(end_date)
        and start_date > end_date
    ):
        warnings.append(f"입사일({start_date})이 퇴사일({end_date})보다 늦습니다.")

    return warnings


def detect_overlapping_periods(periods: list[tuple[str | None, str | None, bool]]) -> list[str]:
    """(start_date, end_date, is_current) 목록에서 겹치는 기간을 찾아 경고를 반환한다.

    end_date가 없고 is_current=True인 경우 오늘까지로 취급해 비교한다.
    """
    warnings: list[str] = []
    normalized = []
    for start, end, is_current in periods:
        if not start or not is_valid_date_ym(start):
            continue
        effective_end = end if (end and is_valid_date_ym(end)) else ("9999-12" if is_current else start)
        normalized.append((start, effective_end))

    for i in range(len(normalized)):
        for j in range(i + 1, len(normalized)):
            s1, e1 = normalized[i]
            s2, e2 = normalized[j]
            if s1 <= e2 and s2 <= e1:
                warnings.append(f"경력 기간이 겹칩니다: {s1}~{e1} / {s2}~{e2}")

    return warnings
