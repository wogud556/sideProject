"""DART 화면(기업 매칭/기업 상세) — 얇은 뷰, 로직은 services/에 위임."""
import re

from django.contrib import messages
from django.http import Http404
from django.shortcuts import redirect, render
from django.utils import timezone

from apps.accounts.decorators import login_required
from apps.pdf_analysis.schemas.common import DartMatchStatus
from apps.resumes import permissions as resume_permissions
from apps.resumes import repository as resume_repository

from . import constants, exceptions
from .services import company_service, linking_service

CORP_CODE_RE = re.compile(constants.CORP_CODE_PATTERN)

DART_SOURCE_NOTICE = (
    "본 기업정보·재무정보는 금융감독원 전자공시시스템(DART)에서 제공하는 공개 데이터를 "
    "기반으로 하며, 최신 공시와 차이가 있을 수 있습니다."
)


@login_required
def career_search_view(request, resume_id, index):
    user_id = request.current_user["id"]
    resume_permissions.get_owned_document_or_404(resume_id, user_id)
    profile_data = resume_repository.read_profile(resume_id) or {}
    careers = profile_data.get("careers", [])
    if index < 0 or index >= len(careers):
        raise Http404("경력 항목을 찾을 수 없습니다.")

    if request.method == "POST":
        action = request.POST.get("action")
        corp_code = request.POST.get("corp_code", "")
        if action == "select":
            try:
                linking_service.link_career_to_company(
                    resume_id, user_id, index, corp_code, DartMatchStatus.MATCHED_MANUAL
                )
                messages.success(request, "기업을 연결했습니다.")
            except ValueError as exc:
                messages.error(request, str(exc))
        elif action == "confirm_auto":
            # 자동 매칭 후보를 화면에서 확인한 뒤 명시적으로 연결한다(GET에서는
            # 후보를 보여주기만 하고 절대 쓰기를 하지 않는다 — CSRF로 트리거되는
            # 부수효과 방지).
            try:
                linking_service.link_career_to_company(
                    resume_id, user_id, index, corp_code, DartMatchStatus.MATCHED_AUTO
                )
                messages.success(request, "기업을 자동 연결했습니다.")
            except ValueError as exc:
                messages.error(request, str(exc))
        elif action == "unlink":
            linking_service.unlink_career(resume_id, user_id, index)
            messages.success(request, "연결을 해제했습니다.")
        return redirect("dart:career_search", resume_id=resume_id, index=index)

    career = careers[index]
    keyword = request.GET.get("keyword", "").strip()

    if keyword:
        candidates = company_service.search_corporations(keyword)
        search_result = {"status": None, "candidates": candidates}
    else:
        # 읽기 전용 조회만 수행한다 — 자동 매칭 후보가 있어도 여기서는 절대
        # profile.json에 쓰지 않는다. 사용자가 "자동 매칭 연결하기" 버튼으로
        # 명시적 POST를 보내야만 실제로 연결된다.
        search_result = linking_service.search_candidates(career.get("company_name_raw", ""))

    return render(
        request,
        "dart/company_search.html",
        {
            "resume_id": resume_id,
            "index": index,
            "career": career,
            "keyword": keyword,
            "search_result": search_result,
        },
    )


@login_required
def company_detail_view(request, corp_code):
    if not CORP_CODE_RE.match(corp_code):
        raise Http404("올바르지 않은 기업 코드입니다.")

    profile = None
    profile_error = None
    try:
        profile = company_service.get_company_profile(corp_code)
    except exceptions.DartError as exc:
        profile_error = str(exc)

    business_year = request.GET.get("year") or str(timezone.now().year - 1)
    financials = None
    financials_error = None
    try:
        financials = company_service.get_financial_accounts(corp_code, business_year)
    except exceptions.DartError as exc:
        financials_error = str(exc)

    return render(
        request,
        "dart/company_detail.html",
        {
            "corp_code": corp_code,
            "profile": profile,
            "profile_error": profile_error,
            "financials": financials,
            "financials_error": financials_error,
            "business_year": business_year,
            "source_notice": DART_SOURCE_NOTICE,
        },
    )
