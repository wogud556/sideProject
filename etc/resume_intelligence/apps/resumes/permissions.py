"""이력서 소유자 검사.

비소유자(또는 존재하지 않는 resume_id) 접근은 항상 404로 응답한다
(존재 여부를 노출하지 않기 위해 403 대신 404 사용).
"""
from django.http import Http404

from . import repository


def get_owned_document_or_404(resume_id, user_id: str) -> dict:
    document = repository.read_document(str(resume_id))
    if document is None or document.get("user_id") != user_id:
        raise Http404("이력서를 찾을 수 없습니다.")
    return document
