"""data/resumes/{resume_id}/ 기반 저장소.

repository 계층은 dict in/out만 다룬다 (Pydantic 검증/직렬화는 service
계층 책임). settings.DATA_DIR은 매 호출 시점에 참조한다.
"""
import shutil
from pathlib import Path
from typing import Any

from django.conf import settings

from storage.json_store import read_json, update_json, write_json


def resumes_root() -> Path:
    return settings.DATA_DIR / "resumes"


def resume_dir(resume_id: str) -> Path:
    return resumes_root() / str(resume_id)


def document_path(resume_id: str) -> Path:
    return resume_dir(resume_id) / "document.json"


def blocks_path(resume_id: str) -> Path:
    return resume_dir(resume_id) / "blocks.json"


def profile_path(resume_id: str) -> Path:
    return resume_dir(resume_id) / "profile.json"


def original_pdf_path(resume_id: str) -> Path:
    return resume_dir(resume_id) / "original.pdf"


def read_document(resume_id: str) -> dict[str, Any] | None:
    return read_json(document_path(resume_id))


def write_document(resume_id: str, data: dict[str, Any]) -> None:
    write_json(document_path(resume_id), data)


def update_document(resume_id: str, mutate_fn, default=None):
    return update_json(document_path(resume_id), mutate_fn, default)


def read_blocks(resume_id: str) -> list[Any]:
    return read_json(blocks_path(resume_id), default=[])


def write_blocks(resume_id: str, data: list[Any]) -> None:
    write_json(blocks_path(resume_id), data)


def read_profile(resume_id: str) -> dict[str, Any] | None:
    return read_json(profile_path(resume_id))


def write_profile(resume_id: str, data: dict[str, Any]) -> None:
    write_json(profile_path(resume_id), data)


def update_profile(resume_id: str, mutate_fn, default=None):
    """profile.json에 대한 read-modify-write를 경로별 락으로 직렬화한다.

    mutate_fn(current_dict_or_default) -> new_dict 형태. careers_service의
    모든 편집 함수는 반드시 이 함수를 통해서만 profile.json을 갱신해야
    한다(개별 read_profile+write_profile 조합은 동시 요청 시
    lost-update를 유발한다).
    """
    return update_json(profile_path(resume_id), mutate_fn, default)


def save_original_pdf(resume_id: str, content: bytes) -> None:
    path = original_pdf_path(resume_id)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)


def list_all_resume_ids() -> list[str]:
    root = resumes_root()
    if not root.exists():
        return []
    return [p.name for p in root.iterdir() if p.is_dir() and (p / "document.json").exists()]


def list_documents_for_user(user_id: str) -> list[dict[str, Any]]:
    documents = []
    for resume_id in list_all_resume_ids():
        document = read_document(resume_id)
        if document and document.get("user_id") == user_id:
            documents.append(document)
    return documents


def find_by_hash(user_id: str, file_hash: str) -> dict[str, Any] | None:
    for document in list_documents_for_user(user_id):
        if document.get("file_hash") == file_hash:
            return document
    return None


def delete_resume(resume_id: str) -> None:
    path = resume_dir(resume_id)
    if path.exists():
        shutil.rmtree(path)
