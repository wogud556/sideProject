"""이력서 업로드 → 검증 → 저장 → 동기 분석 오케스트레이션.

apps.pdf_analysis는 순수 라이브러리(스스로 저장하지 않음)이므로, 실제
data/resumes/{resume_id}/ 아래에 파일을 쓰는 책임은 여기(서비스 계층)에
있다.

document.json에 대한 모든 상태 갱신은 repository.update_document(경로별
락 + read-modify-write)를 통해서만 수행한다 — 개별 read_document()/
write_document() 조합은 동시 요청(예: 업로드 중 재분석 요청) 시
lost-update를 유발하므로 사용하지 않는다.
"""
import uuid
from datetime import datetime, timezone

from apps.pdf_analysis.pipeline import run_pipeline
from apps.pdf_analysis.schemas.document import AnalysisStatus
from apps.pdf_analysis.schemas.profile import ResumeProfile
from apps.pdf_analysis.validators.file_validators import validate_uploaded_pdf

from .. import permissions, repository

# PdfValidationError를 그대로 재노출한다(호출자가 apps.pdf_analysis를 직접
# import하지 않아도 예외를 처리할 수 있도록).
from apps.pdf_analysis.validators.file_validators import PdfValidationError  # noqa: F401


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _sanitize_filename(name: str) -> str:
    """깨진 인코딩(예: 잘못된 멀티파트 헤더로 생긴 lone surrogate)이 섞인
    파일명을 JSON(UTF-8)에 안전하게 저장할 수 있도록 정리한다. 서로게이트가
    남아 있으면 json.dump(ensure_ascii=False)가 UnicodeEncodeError로 죽는다."""
    return name.encode("utf-8", errors="replace").decode("utf-8")


def upload_resume(user_id: str, uploaded_file) -> str:
    """PDF를 검증·저장하고 동기적으로 분석까지 수행한 뒤 resume_id를 반환한다.

    검증 실패(PdfValidationError)는 레코드를 만들지 않고 그대로 전파한다.
    검증 통과 후 파이프라인 실행 중 예외가 발생하면 FAILED 상태로 기록하고
    반환한다(업로드 자체는 성공했으므로 예외를 다시 던지지 않는다).
    """
    info = validate_uploaded_pdf(uploaded_file)

    resume_id = str(uuid.uuid4())

    uploaded_file.seek(0)
    content = uploaded_file.read()
    repository.save_original_pdf(resume_id, content)

    warnings: list[str] = []
    if repository.find_by_hash(user_id, info.file_hash) is not None:
        warnings.append("DUPLICATE_FILE_HASH_DETECTED")

    document = {
        "id": resume_id,
        "user_id": user_id,
        "original_filename": _sanitize_filename(getattr(uploaded_file, "name", "resume.pdf") or "resume.pdf"),
        "file_size": info.file_size,
        "file_hash": info.file_hash,
        "page_count": info.page_count,
        "is_encrypted": info.is_encrypted,
        "analysis_status": AnalysisStatus.UPLOADED.value,
        "ocr_used": False,
        "uploaded_at": _now_iso(),
        "analysis_completed_at": None,
        "error_code": None,
        "error_message": None,
        "warnings": warnings,
    }
    # 최초 생성이므로 아직 동시 접근 대상이 아니다 — write_document로 충분하다.
    repository.write_document(resume_id, document)

    _run_analysis(resume_id)

    return resume_id


def _set_status(resume_id: str, **fields) -> None:
    """document.json의 일부 필드를 락으로 보호된 read-modify-write로 갱신한다."""

    def mutate(current):
        document = dict(current or {})
        document.update(fields)
        return document

    repository.update_document(resume_id, mutate, default={})


def _run_analysis(resume_id: str) -> None:
    _set_status(resume_id, analysis_status=AnalysisStatus.EXTRACTING.value)

    pdf_path = repository.original_pdf_path(resume_id)

    try:
        result = run_pipeline(str(pdf_path))
    except Exception:
        _set_status(
            resume_id,
            analysis_status=AnalysisStatus.FAILED.value,
            error_code="PIPELINE_ERROR",
            error_message="이력서 분석 중 오류가 발생했습니다.",
            analysis_completed_at=_now_iso(),
        )
        return

    _set_status(resume_id, analysis_status=AnalysisStatus.STRUCTURING.value)

    repository.write_blocks(resume_id, [b.model_dump(mode="json") for b in result.blocks])
    repository.write_profile(resume_id, result.profile.model_dump(mode="json"))

    def finalize(current):
        document = dict(current or {})
        all_warnings = list(document.get("warnings") or []) + list(result.warnings)
        final_status = (
            AnalysisStatus.COMPLETED_WITH_WARNINGS if all_warnings else AnalysisStatus.COMPLETED
        )
        document["analysis_status"] = final_status.value
        document["ocr_used"] = result.ocr_used
        document["warnings"] = all_warnings
        document["analysis_completed_at"] = _now_iso()
        return document

    repository.update_document(resume_id, finalize, default={})


def reanalyze_resume(resume_id: str, user_id: str, overwrite_user_edits: bool = False) -> None:
    """원본 PDF를 다시 분석한다. 기본값(overwrite_user_edits=False)에서는
    사용자가 이미 채워둔 값을 덮어쓰지 않는다."""
    permissions.get_owned_document_or_404(resume_id, user_id)

    _set_status(resume_id, analysis_status=AnalysisStatus.EXTRACTING.value)

    pdf_path = repository.original_pdf_path(resume_id)

    try:
        result = run_pipeline(str(pdf_path))
    except Exception:
        _set_status(
            resume_id,
            analysis_status=AnalysisStatus.FAILED.value,
            error_code="PIPELINE_ERROR",
            error_message="재분석 중 오류가 발생했습니다.",
            analysis_completed_at=_now_iso(),
        )
        return

    new_profile = result.profile
    if not overwrite_user_edits:
        existing_profile_dict = repository.read_profile(resume_id)
        if existing_profile_dict:
            existing_profile = ResumeProfile.model_validate(existing_profile_dict)
            new_profile = _merge_preserving_user_edits(existing_profile, new_profile)

    repository.write_blocks(resume_id, [b.model_dump(mode="json") for b in result.blocks])
    repository.write_profile(resume_id, new_profile.model_dump(mode="json"))

    _set_status(
        resume_id,
        analysis_status=(
            AnalysisStatus.COMPLETED_WITH_WARNINGS.value if result.warnings else AnalysisStatus.COMPLETED.value
        ),
        ocr_used=result.ocr_used,
        warnings=result.warnings,
        analysis_completed_at=_now_iso(),
    )


def _merge_preserving_user_edits(old: ResumeProfile, new: ResumeProfile) -> ResumeProfile:
    """overwrite_user_edits=False일 때 사용자의 기존 값을 보존하는 병합 규칙.

    - basic: 기존 값(value)이 채워진 필드는 유지, 비어 있으면 새 값으로 채운다.
    - careers/educations/projects/certificates/skills/languages: 기존
      리스트가 비어 있지 않으면(=사용자가 이미 다뤘을 가능성) 그대로 유지,
      비어 있으면 새로 추출된 값을 사용한다.
    - is_user_confirmed/confirmed_at은 항상 기존 값을 보존한다.
    """
    merged = new.model_copy(deep=True)

    basic_data = old.basic.model_dump(mode="json")
    new_basic_data = new.basic.model_dump(mode="json")
    for field_name, field_value in basic_data.items():
        if field_value.get("value") is None:
            basic_data[field_name] = new_basic_data[field_name]
    merged.basic = type(old.basic).model_validate(basic_data)

    for list_name in ("careers", "educations", "projects", "certificates", "skills", "languages"):
        old_list = getattr(old, list_name)
        if old_list:
            setattr(merged, list_name, old_list)

    merged.is_user_confirmed = old.is_user_confirmed
    merged.confirmed_at = old.confirmed_at
    return merged
