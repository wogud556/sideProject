"""경력 ↔ DART 기업 연결.

apps.resumes.services.careers_service를 경유해 profile.json을 갱신한다
(resumes 앱은 dart를 import하지 않지만, dart가 resumes를 사용하는 것은
허용된다 — 의존 방향은 dart → resumes 단방향).
"""
import re

from apps.pdf_analysis.schemas.common import DartMatchStatus
from apps.resumes.services import careers_service

from .. import repository
from ..constants import CORP_CODE_PATTERN
from ..matching import match_company

_CORP_CODE_RE = re.compile(CORP_CODE_PATTERN)


def search_candidates(company_name_raw: str) -> dict:
    """경력의 기업명으로 DART 후보를 찾는다."""
    corporations = repository.read_corporations()
    if not corporations:
        return {
            "status": DartMatchStatus.ERROR.value,
            "candidates": [],
            "message": "DART 기업 정보가 아직 동기화되지 않았습니다. "
            "관리자가 `python manage.py sync_dart_corporations`를 실행해야 합니다.",
        }
    result = match_company(company_name_raw, corporations)
    return {"status": result["status"].value, "candidates": result["candidates"]}


def link_career_to_company(
    resume_id,
    user_id: str,
    career_index: int,
    corp_code: str,
    match_status: DartMatchStatus = DartMatchStatus.MATCHED_MANUAL,
) -> dict:
    if not _CORP_CODE_RE.match(corp_code):
        raise ValueError("corp_code 형식이 올바르지 않습니다 (8자리 숫자여야 합니다).")

    patch = {"dart_corp_code": corp_code, "dart_match_status": match_status.value}
    return careers_service.update_entry(resume_id, user_id, "careers", career_index, patch)


def unlink_career(resume_id, user_id: str, career_index: int) -> dict:
    patch = {"dart_corp_code": None, "dart_match_status": DartMatchStatus.NOT_SEARCHED.value}
    return careers_service.update_entry(resume_id, user_id, "careers", career_index, patch)
