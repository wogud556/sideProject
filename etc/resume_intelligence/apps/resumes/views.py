"""이력서 화면(대시보드/업로드/상세) — 얇은 뷰, 로직은 services/에 위임."""
from django.contrib import messages
from django.http import FileResponse, Http404, HttpResponse
from django.shortcuts import redirect, render
from django.urls import reverse
from django.views.decorators.http import require_http_methods

from apps.accounts.decorators import login_required
from apps.pdf_analysis.schemas.common import DartMatchStatus
from apps.pdf_analysis.schemas.document import AnalysisStatus
from apps.pdf_analysis.schemas.profile import ResumeProfile
from apps.pdf_analysis.validators.file_validators import PdfValidationError

from . import permissions, repository
from .forms import ResumeUploadForm
from .services import careers_service, excel_export, upload_service

MATCHED_STATUSES = {DartMatchStatus.MATCHED_AUTO.value, DartMatchStatus.MATCHED_MANUAL.value}


def _resume_needs_dart_match(resume_id: str) -> bool:
    profile_data = repository.read_profile(resume_id)
    if not profile_data:
        return False
    for career in profile_data.get("careers", []):
        if career.get("company_name_raw") and career.get("dart_match_status") not in MATCHED_STATUSES:
            return True
    return False


@login_required
def dashboard_view(request):
    user_id = request.current_user["id"]
    documents = repository.list_documents_for_user(user_id)
    documents.sort(key=lambda d: d.get("uploaded_at") or "", reverse=True)

    completed_statuses = {AnalysisStatus.COMPLETED.value, AnalysisStatus.COMPLETED_WITH_WARNINGS.value}
    stats = {
        "total": len(documents),
        "completed": sum(1 for d in documents if d.get("analysis_status") in completed_statuses),
        "failed": sum(1 for d in documents if d.get("analysis_status") == AnalysisStatus.FAILED.value),
        "match_needed": sum(1 for d in documents if _resume_needs_dart_match(d["id"])),
    }

    return render(
        request,
        "resumes/dashboard.html",
        {"documents": documents[:20], "stats": stats},
    )


@login_required
@require_http_methods(["GET", "POST"])
def upload_view(request):
    if request.method == "POST":
        form = ResumeUploadForm(request.POST, request.FILES)
        if form.is_valid():
            try:
                resume_id = upload_service.upload_resume(
                    request.current_user["id"], form.cleaned_data["file"]
                )
            except PdfValidationError as exc:
                form.add_error("file", exc.message)
            else:
                messages.success(request, "이력서 분석이 완료되었습니다.")
                return redirect("resumes:detail", resume_id=resume_id)
    else:
        form = ResumeUploadForm()
    return render(request, "resumes/upload.html", {"form": form})


@login_required
def detail_view(request, resume_id):
    user_id = request.current_user["id"]
    document = permissions.get_owned_document_or_404(resume_id, user_id)
    profile_data = repository.read_profile(resume_id) or {}
    profile = ResumeProfile.model_validate(profile_data)

    return render(
        request,
        "resumes/detail.html",
        {
            "resume_id": resume_id,
            "document": document,
            "profile": profile,
        },
    )


@login_required
@require_http_methods(["POST"])
def delete_view(request, resume_id):
    permissions.get_owned_document_or_404(resume_id, request.current_user["id"])
    repository.delete_resume(str(resume_id))
    messages.success(request, "이력서를 삭제했습니다.")
    return redirect("resumes:dashboard")


@login_required
@require_http_methods(["POST"])
def confirm_view(request, resume_id):
    careers_service.confirm_resume(resume_id, request.current_user["id"])
    messages.success(request, "확인 완료로 표시했습니다.")
    return redirect("resumes:detail", resume_id=resume_id)


@login_required
@require_http_methods(["POST"])
def reanalyze_view(request, resume_id):
    upload_service.reanalyze_resume(resume_id, request.current_user["id"], overwrite_user_edits=False)
    messages.success(request, "재분석을 완료했습니다.")
    return redirect("resumes:detail", resume_id=resume_id)


@login_required
def pdf_view(request, resume_id):
    document = permissions.get_owned_document_or_404(resume_id, request.current_user["id"])
    path = repository.original_pdf_path(resume_id)
    if not path.exists():
        raise Http404("원본 PDF를 찾을 수 없습니다.")
    return FileResponse(open(path, "rb"), content_type="application/pdf", filename=document["original_filename"])


@login_required
def raw_text_view(request, resume_id):
    document = permissions.get_owned_document_or_404(resume_id, request.current_user["id"])
    blocks = repository.read_blocks(resume_id)
    return render(
        request,
        "resumes/raw_text.html",
        {"resume_id": resume_id, "document": document, "blocks": blocks},
    )


@login_required
def excel_view(request, resume_id):
    buffer = excel_export.export_resume_to_excel(resume_id, request.current_user["id"])
    response = HttpResponse(
        buffer.read(),
        content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )
    response["Content-Disposition"] = f'attachment; filename="resume_{resume_id}.xlsx"'
    return response


@login_required
@require_http_methods(["POST"])
def basic_info_edit_view(request, resume_id):
    basic_patch = {
        key: request.POST[key]
        for key in ("name", "name_en", "birth_date", "email", "phone", "address", "portfolio_url", "github_url", "linkedin_url", "summary")
        if key in request.POST
    }
    careers_service.patch_profile(resume_id, request.current_user["id"], {"basic": basic_patch})
    messages.success(request, "기본정보를 저장했습니다.")
    return redirect(reverse("resumes:detail", args=[resume_id]) + "#basic")


def _responsibilities_from_post(request) -> list[str]:
    raw = request.POST.get("responsibilities", "")
    return [line.strip() for line in raw.splitlines() if line.strip()]


@login_required
@require_http_methods(["POST"])
def career_add_view(request, resume_id):
    data = {
        "company_name_raw": request.POST.get("company_name_raw", ""),
        "department": request.POST.get("department") or None,
        "position": request.POST.get("position") or None,
        "start_date": request.POST.get("start_date") or None,
        "end_date": request.POST.get("end_date") or None,
        "is_current": bool(request.POST.get("is_current")),
        "responsibilities": _responsibilities_from_post(request),
        "confidence": 1.0,
        "review_required": False,
    }
    careers_service.add_career(resume_id, request.current_user["id"], data)
    messages.success(request, "경력을 추가했습니다.")
    return redirect(reverse("resumes:detail", args=[resume_id]) + "#careers")


@login_required
@require_http_methods(["POST"])
def career_delete_view(request, resume_id, index):
    careers_service.delete_career(resume_id, request.current_user["id"], index)
    messages.success(request, "경력을 삭제했습니다.")
    return redirect(reverse("resumes:detail", args=[resume_id]) + "#careers")


@login_required
@require_http_methods(["POST"])
def education_add_view(request, resume_id):
    data = {
        "school_name": request.POST.get("school_name", ""),
        "major": request.POST.get("major") or None,
        "degree": request.POST.get("degree") or None,
        "start_date": request.POST.get("start_date") or None,
        "end_date": request.POST.get("end_date") or None,
        "confidence": 1.0,
        "review_required": False,
    }
    careers_service.add_education(resume_id, request.current_user["id"], data)
    messages.success(request, "학력을 추가했습니다.")
    return redirect(reverse("resumes:detail", args=[resume_id]) + "#educations")


@login_required
@require_http_methods(["POST"])
def education_delete_view(request, resume_id, index):
    careers_service.delete_education(resume_id, request.current_user["id"], index)
    messages.success(request, "학력을 삭제했습니다.")
    return redirect(reverse("resumes:detail", args=[resume_id]) + "#educations")
