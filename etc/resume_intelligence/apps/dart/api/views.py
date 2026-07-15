"""SPEC.md 5장 DART 관련 /api/v1/ 엔드포인트 (JSON). 얇은 뷰.

세션 쿠키 인증을 쓰는 JSON API이므로 상태 변경 엔드포인트는 CSRF 검사를
그대로 받는다 — 클라이언트는 X-CSRFToken 헤더를 보내야 한다.
"""
import json
import re

from django.http import JsonResponse
from django.utils import timezone
from django.views.decorators.http import require_http_methods

from apps.accounts.decorators import api_login_required
from apps.pdf_analysis.schemas.common import DartMatchStatus

from .. import constants, exceptions
from ..services import company_service, linking_service

CORP_CODE_RE = re.compile(constants.CORP_CODE_PATTERN)

_ERROR_STATUS_MAP = {
    exceptions.DartApiKeyMissing: ("DART_API_KEY_MISSING", 503),
    exceptions.DartNotFound: ("DART_NOT_FOUND", 404),
    exceptions.DartRateLimited: ("DART_RATE_LIMITED", 429),
    exceptions.DartTimeout: ("DART_TIMEOUT", 504),
    exceptions.DartInvalidQuery: ("DART_INVALID_QUERY", 400),
    exceptions.DartApiError: ("DART_API_ERROR", 502),
}


def _body(request) -> dict:
    if not request.body:
        return {}
    try:
        return json.loads(request.body)
    except json.JSONDecodeError:
        return {}


def _dart_error_response(exc: exceptions.DartError) -> JsonResponse:
    for exc_type, (code, status) in _ERROR_STATUS_MAP.items():
        if isinstance(exc, exc_type):
            return JsonResponse({"error_code": code, "message": str(exc)}, status=status)
    return JsonResponse({"error_code": "DART_ERROR", "message": str(exc)}, status=502)


def _invalid_corp_code_response() -> JsonResponse:
    return JsonResponse(
        {"error_code": "INVALID_CORP_CODE", "message": "corp_code는 8자리 숫자여야 합니다."}, status=400
    )


@api_login_required
@require_http_methods(["GET"])
def corporation_search_view(request):
    keyword = request.GET.get("keyword", "").strip()
    if not keyword:
        return JsonResponse({"results": []})
    results = company_service.search_corporations(keyword)
    return JsonResponse({"results": results})


@api_login_required
@require_http_methods(["GET"])
def corporation_detail_view(request, corp_code):
    if not CORP_CODE_RE.match(corp_code):
        return _invalid_corp_code_response()
    try:
        profile = company_service.get_company_profile(corp_code)
    except exceptions.DartError as exc:
        return _dart_error_response(exc)
    return JsonResponse(profile)


@api_login_required
@require_http_methods(["GET"])
def corporation_financials_view(request, corp_code):
    if not CORP_CODE_RE.match(corp_code):
        return _invalid_corp_code_response()
    year = request.GET.get("year") or str(timezone.now().year - 1)
    report_code = request.GET.get("report_code") or constants.REPORT_CODE_ANNUAL
    try:
        financials = company_service.get_financial_accounts(corp_code, year, report_code)
    except exceptions.DartError as exc:
        return _dart_error_response(exc)
    return JsonResponse(financials)


@api_login_required
@require_http_methods(["POST"])
def corporation_refresh_view(request, corp_code):
    if not CORP_CODE_RE.match(corp_code):
        return _invalid_corp_code_response()
    try:
        profile = company_service.refresh_company_profile(corp_code)
    except exceptions.DartError as exc:
        return _dart_error_response(exc)
    return JsonResponse(profile)


@api_login_required
@require_http_methods(["POST"])
def career_dart_company_view(request, resume_id, index):
    user_id = request.current_user["id"]
    body = _body(request)
    corp_code = body.get("corp_code")

    if corp_code is None:
        linking_service.unlink_career(resume_id, user_id, index)
        return JsonResponse({}, status=200)

    try:
        career = linking_service.link_career_to_company(
            resume_id, user_id, index, corp_code, DartMatchStatus.MATCHED_MANUAL
        )
    except ValueError as exc:
        return JsonResponse({"error_code": "INVALID_CORP_CODE", "message": str(exc)}, status=400)
    return JsonResponse(career)
