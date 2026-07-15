"""SPEC.md 5장 /api/v1/resumes... 엔드포인트 (JSON). 얇은 뷰 — 로직은 services/에 위임.

세션 쿠키 인증을 쓰는 JSON API이므로 CSRF 보호가 반드시 필요하다
(django.middleware.csrf.CsrfViewMiddleware가 이미 활성화되어 있음).
POST/PATCH/DELETE 호출 시 클라이언트는 `X-CSRFToken` 헤더에 csrftoken
쿠키 값을 담아 보내야 한다 — README "API 인증/CSRF" 절 참고.
"""
import json

from django.http import HttpResponse, JsonResponse
from django.views.decorators.http import require_http_methods
from pydantic import ValidationError

from apps.accounts.decorators import api_login_required
from apps.pdf_analysis.validators.file_validators import PdfValidationError

from .. import permissions, repository
from ..services import careers_service, excel_export, upload_service


def _body(request) -> dict:
    if not request.body:
        return {}
    try:
        return json.loads(request.body)
    except json.JSONDecodeError:
        return {}


@api_login_required
@require_http_methods(["GET", "POST"])
def resume_list_create_view(request):
    user_id = request.current_user["id"]

    if request.method == "POST":
        uploaded_file = request.FILES.get("file")
        if uploaded_file is None:
            return JsonResponse({"error_code": "MISSING_FILE", "message": "file 필드가 필요합니다."}, status=400)
        try:
            resume_id = upload_service.upload_resume(user_id, uploaded_file)
        except PdfValidationError as exc:
            return JsonResponse({"error_code": exc.code, "message": exc.message}, status=400)
        document = repository.read_document(resume_id)
        return JsonResponse(document, status=201)

    documents = repository.list_documents_for_user(user_id)
    documents.sort(key=lambda d: d.get("uploaded_at") or "", reverse=True)
    return JsonResponse({"results": documents})


@api_login_required
@require_http_methods(["GET"])
def resume_detail_view(request, resume_id):
    user_id = request.current_user["id"]
    document = permissions.get_owned_document_or_404(resume_id, user_id)
    profile = repository.read_profile(resume_id) or {}
    return JsonResponse({"document": document, "profile": profile})


@api_login_required
@require_http_methods(["GET"])
def resume_status_view(request, resume_id):
    user_id = request.current_user["id"]
    document = permissions.get_owned_document_or_404(resume_id, user_id)
    return JsonResponse(
        {
            "id": document["id"],
            "analysis_status": document["analysis_status"],
            "ocr_used": document.get("ocr_used", False),
            "warnings": document.get("warnings", []),
            "error_code": document.get("error_code"),
            "error_message": document.get("error_message"),
        }
    )


@api_login_required
@require_http_methods(["PATCH"])
def resume_profile_patch_view(request, resume_id):
    user_id = request.current_user["id"]
    patch = _body(request)
    try:
        profile = careers_service.patch_profile(resume_id, user_id, patch)
    except ValidationError as exc:
        return JsonResponse({"error_code": "INVALID_PROFILE_DATA", "message": str(exc)}, status=400)
    return JsonResponse(profile)


@api_login_required
@require_http_methods(["POST"])
def resume_confirm_view(request, resume_id):
    user_id = request.current_user["id"]
    profile = careers_service.confirm_resume(resume_id, user_id)
    return JsonResponse(profile)


@api_login_required
@require_http_methods(["POST"])
def resume_reanalyze_view(request, resume_id):
    user_id = request.current_user["id"]
    body = _body(request)
    overwrite = bool(body.get("overwrite_user_edits", False))
    upload_service.reanalyze_resume(resume_id, user_id, overwrite_user_edits=overwrite)
    document = permissions.get_owned_document_or_404(resume_id, user_id)
    return JsonResponse(document)


@api_login_required
@require_http_methods(["GET"])
def resume_excel_view(request, resume_id):
    user_id = request.current_user["id"]
    buffer = excel_export.export_resume_to_excel(resume_id, user_id)
    response = HttpResponse(
        buffer.read(),
        content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )
    response["Content-Disposition"] = f'attachment; filename="resume_{resume_id}.xlsx"'
    return response


def _entry_list_create_view(request, resume_id, list_name: str):
    user_id = request.current_user["id"]
    data = _body(request)
    try:
        entry = careers_service.add_entry(resume_id, user_id, list_name, data)
    except ValidationError as exc:
        return JsonResponse({"error_code": "INVALID_DATA", "message": str(exc)}, status=400)
    return JsonResponse(entry, status=201)


def _entry_detail_view(request, resume_id, index, list_name: str):
    user_id = request.current_user["id"]
    if request.method == "PATCH":
        patch = _body(request)
        try:
            entry = careers_service.update_entry(resume_id, user_id, list_name, index, patch)
        except ValidationError as exc:
            return JsonResponse({"error_code": "INVALID_DATA", "message": str(exc)}, status=400)
        return JsonResponse(entry)

    careers_service.delete_entry(resume_id, user_id, list_name, index)
    return HttpResponse(status=204)


@api_login_required
@require_http_methods(["POST"])
def career_list_create_view(request, resume_id):
    return _entry_list_create_view(request, resume_id, "careers")


@api_login_required
@require_http_methods(["PATCH", "DELETE"])
def career_detail_view(request, resume_id, index):
    return _entry_detail_view(request, resume_id, index, "careers")


@api_login_required
@require_http_methods(["POST"])
def education_list_create_view(request, resume_id):
    return _entry_list_create_view(request, resume_id, "educations")


@api_login_required
@require_http_methods(["PATCH", "DELETE"])
def education_detail_view(request, resume_id, index):
    return _entry_detail_view(request, resume_id, index, "educations")
