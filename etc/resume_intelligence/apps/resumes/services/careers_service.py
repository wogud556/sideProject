"""경력·학력 추가/수정/삭제 및 나머지 프로필 편집(PATCH)·확인 완료.

careers/educations는 리스트 내 위치(index)를 안정적인 식별자로 사용한다
(SPEC.md 2장 필드 목록에 별도 id가 없으므로 스키마를 건드리지 않기 위함).
그 외 탭(기본정보/프로젝트/자격증/기술/어학)은 profile 전체 PATCH로 편집한다.

모든 편집 함수는 repository.update_profile(경로별 락 + read-modify-write)을
통해서만 profile.json을 갱신한다 — 개별 read_profile()/write_profile() 조합은
동시 요청 시 lost-update를 유발하므로 사용하지 않는다.
"""
import re
from datetime import datetime, timezone

from django.http import Http404

from apps.pdf_analysis.schemas.profile import Career, Education, ResumeProfile

from .. import permissions, repository

_LIST_MODELS = {"careers": Career, "educations": Education}

# apps.dart.normalization.normalize_company_name과 동일한 규칙의 최소 구현.
# resumes 앱은 apps.dart를 import하지 않는 원칙(dart → resumes 단방향 의존)을
# 지키기 위해 이 최소 구현을 별도로 둔다. DART 매칭에 실제로 쓰이는 정식
# 구현은 apps.dart.normalization.normalize_company_name이다.
_COMPANY_SUFFIX_RE = re.compile(r"주식회사|㈜|\(주\)|유한회사")
_STRIP_CHARS_RE = re.compile(r"[\s\-·.,()\[\]{}'\"]+")


def _normalize_company_name(name: str | None) -> str:
    if not name:
        return ""
    normalized = _COMPANY_SUFFIX_RE.sub("", name)
    normalized = _STRIP_CHARS_RE.sub("", normalized)
    return normalized.lower()


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


# ----------------------------------------------------------------------
# careers / educations 공통 로직
# ----------------------------------------------------------------------
def add_entry(resume_id, user_id: str, list_name: str, item_data: dict) -> dict:
    permissions.get_owned_document_or_404(resume_id, user_id)
    model_cls = _LIST_MODELS[list_name]

    created: dict[str, Career | Education] = {}

    def mutate(current):
        profile = ResumeProfile.model_validate(current or {})
        items = getattr(profile, list_name)

        item = model_cls.model_validate(item_data)
        item.sort_order = len(items)
        items.append(item)
        created["item"] = item

        return profile.model_dump(mode="json")

    repository.update_profile(resume_id, mutate, default={})
    return created["item"].model_dump(mode="json")


def update_entry(resume_id, user_id: str, list_name: str, index: int, patch: dict) -> dict:
    permissions.get_owned_document_or_404(resume_id, user_id)
    model_cls = _LIST_MODELS[list_name]

    updated: dict[str, Career | Education] = {}

    def mutate(current):
        profile = ResumeProfile.model_validate(current or {})
        items = getattr(profile, list_name)

        if index < 0 or index >= len(items):
            raise Http404("항목을 찾을 수 없습니다.")

        current_data = items[index].model_dump(mode="json")
        current_data.update(patch)
        updated_item = model_cls.model_validate(current_data)
        updated_item.sort_order = index
        items[index] = updated_item
        updated["item"] = updated_item

        return profile.model_dump(mode="json")

    repository.update_profile(resume_id, mutate, default={})
    return updated["item"].model_dump(mode="json")


def delete_entry(resume_id, user_id: str, list_name: str, index: int) -> None:
    permissions.get_owned_document_or_404(resume_id, user_id)

    def mutate(current):
        profile = ResumeProfile.model_validate(current or {})
        items = getattr(profile, list_name)

        if index < 0 or index >= len(items):
            raise Http404("항목을 찾을 수 없습니다.")

        del items[index]
        for i, item in enumerate(items):
            item.sort_order = i

        return profile.model_dump(mode="json")

    repository.update_profile(resume_id, mutate, default={})


def add_career(resume_id, user_id, data):
    data = dict(data)
    # company_name_raw만 채워져 있고 정규화명이 없으면 최소 구현으로 채운다
    # (뷰에서 raw를 그대로 복사해 넣는 실수를 방지).
    if data.get("company_name_raw") and not data.get("company_name_normalized"):
        data["company_name_normalized"] = _normalize_company_name(data["company_name_raw"])
    return add_entry(resume_id, user_id, "careers", data)


def update_career(resume_id, user_id, index, patch):
    patch = dict(patch)
    if patch.get("company_name_raw") and "company_name_normalized" not in patch:
        patch["company_name_normalized"] = _normalize_company_name(patch["company_name_raw"])
    return update_entry(resume_id, user_id, "careers", index, patch)


def delete_career(resume_id, user_id, index):
    return delete_entry(resume_id, user_id, "careers", index)


def add_education(resume_id, user_id, data):
    return add_entry(resume_id, user_id, "educations", data)


def update_education(resume_id, user_id, index, patch):
    return update_entry(resume_id, user_id, "educations", index, patch)


def delete_education(resume_id, user_id, index):
    return delete_entry(resume_id, user_id, "educations", index)


# ----------------------------------------------------------------------
# 프로필 전체 PATCH (기본정보/프로젝트/자격증/기술/어학) + 확인 완료
# ----------------------------------------------------------------------
def patch_profile(resume_id, user_id: str, patch: dict) -> dict:
    """basic은 필드 단위(value만 또는 {value,...} 전체)로 병합하고,
    projects/certificates/skills/languages는 리스트를 통째로 교체한다."""
    permissions.get_owned_document_or_404(resume_id, user_id)

    def mutate(current):
        profile = ResumeProfile.model_validate(current or {})
        current_data = profile.model_dump(mode="json")

        basic_patch = patch.get("basic")
        if isinstance(basic_patch, dict):
            for field_name, value in basic_patch.items():
                if field_name not in current_data["basic"]:
                    continue
                if isinstance(value, dict):
                    current_data["basic"][field_name].update(value)
                else:
                    current_data["basic"][field_name]["value"] = value

        for list_name in ("projects", "certificates", "skills", "languages"):
            if isinstance(patch.get(list_name), list):
                current_data[list_name] = patch[list_name]

        if "is_user_confirmed" in patch:
            current_data["is_user_confirmed"] = bool(patch["is_user_confirmed"])

        updated_profile = ResumeProfile.model_validate(current_data)
        return updated_profile.model_dump(mode="json")

    return repository.update_profile(resume_id, mutate, default={})


def confirm_resume(resume_id, user_id: str) -> dict:
    permissions.get_owned_document_or_404(resume_id, user_id)

    def mutate(current):
        profile = ResumeProfile.model_validate(current or {})
        profile.is_user_confirmed = True
        profile.confirmed_at = _now_iso()
        return profile.model_dump(mode="json")

    return repository.update_profile(resume_id, mutate, default={})
