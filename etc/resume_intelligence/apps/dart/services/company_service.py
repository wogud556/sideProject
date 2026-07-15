"""기업개황/재무정보 조회 — 캐시 우선, CFS→OFS 폴백."""
import dataclasses
import re

from .. import constants, exceptions, repository
from ..clients.open_dart_client import OpenDartClient
from ..normalization import normalize_company_name

_YEAR_RE = re.compile(constants.YEAR_PATTERN)


def validate_financials_query(business_year: str, report_code: str) -> None:
    """캐시 파일명(corp_code_{year}_{report}.json) 조합 전 파라미터를 검증한다(MED-6)."""
    if not business_year or not _YEAR_RE.match(business_year):
        raise exceptions.DartInvalidQuery(f"올바르지 않은 연도입니다: {business_year!r}")
    if report_code not in constants.VALID_REPORT_CODES:
        raise exceptions.DartInvalidQuery(f"올바르지 않은 보고서 코드입니다: {report_code!r}")


def search_corporations(keyword: str, limit: int = 20) -> list[dict]:
    """동기화된 corporations.json에서 키워드로 기업을 검색한다(자유 검색창용)."""
    normalized_keyword = normalize_company_name(keyword)
    if not normalized_keyword:
        return []
    corporations = repository.read_corporations()
    matches = [c for c in corporations if normalized_keyword in c.get("normalized_name", "")]
    return matches[:limit]


def get_company_profile(corp_code: str, client=None, force_refresh: bool = False) -> dict:
    """기업개황을 캐시 우선으로 조회한다. 캐시가 없거나 force_refresh면 API 호출."""
    if not force_refresh:
        cached = repository.read_company_profile(corp_code)
        if cached is not None:
            return cached

    client = client or OpenDartClient()
    dto = client.get_company(corp_code)
    data = dataclasses.asdict(dto)
    repository.write_company_profile(corp_code, data)
    return data


def get_financial_accounts(
    corp_code: str,
    business_year: str,
    report_code: str = constants.REPORT_CODE_ANNUAL,
    client=None,
    force_refresh: bool = False,
) -> dict:
    """CFS(연결재무제표) 우선 조회, 데이터가 없으면 OFS(별도재무제표)로 폴백."""
    validate_financials_query(business_year, report_code)

    if not force_refresh:
        cached = repository.read_financials(corp_code, business_year, report_code)
        if cached is not None:
            return cached

    client = client or OpenDartClient()
    try:
        dto = client.get_financial_accounts(
            corp_code, business_year, report_code, constants.FS_DIV_CONSOLIDATED
        )
        if not dto.accounts:
            raise exceptions.DartNotFound("연결재무제표에서 주요 계정을 찾지 못했습니다.")
    except exceptions.DartNotFound:
        dto = client.get_financial_accounts(
            corp_code, business_year, report_code, constants.FS_DIV_SEPARATE
        )

    data = dataclasses.asdict(dto)
    repository.write_financials(corp_code, business_year, report_code, data)
    return data


def refresh_company_profile(corp_code: str, client=None) -> dict:
    return get_company_profile(corp_code, client=client, force_refresh=True)
