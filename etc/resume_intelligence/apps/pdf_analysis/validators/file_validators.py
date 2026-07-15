"""업로드된 PDF 파일 검증 (SPEC.md 3장 / 8장).

확장자 → MIME → PDF 시그니처(%PDF-) → 크기(20MB) → 페이지 수(30) →
암호화 여부 → 빈 PDF 순으로 검사한다. 파일 원문을 로그에 남기지 않는다.
"""
import hashlib

import fitz
from pydantic import BaseModel

MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024
MAX_PAGE_COUNT = 30
PDF_SIGNATURE = b"%PDF-"


class PdfValidationError(Exception):
    """PDF 업로드 검증 실패. code는 화면/API에서 안내 문구로 매핑된다."""

    def __init__(self, code: str, message: str):
        self.code = code
        self.message = message
        super().__init__(message)


class PdfFileInfo(BaseModel):
    file_size: int
    file_hash: str
    page_count: int
    is_encrypted: bool


def _is_blank_document(doc: "fitz.Document") -> bool:
    """모든 페이지에 텍스트도 이미지도 없으면 True (빈 PDF로 간주)."""
    for page in doc:
        if page.get_text().strip():
            return False
        if page.get_images():
            return False
    return True


def validate_uploaded_pdf(uploaded_file) -> PdfFileInfo:
    """Django UploadedFile 유사 객체(.name/.size/.content_type/.read)를 검증한다.

    검증 통과 시 PdfFileInfo를 반환하고, 실패 시 PdfValidationError를 발생시킨다.
    호출 후 uploaded_file의 커서는 처음으로 되돌려진다.
    """
    name = getattr(uploaded_file, "name", "") or ""
    if not name.lower().endswith(".pdf"):
        raise PdfValidationError("INVALID_EXTENSION", "PDF 파일만 업로드할 수 있습니다.")

    content_type = getattr(uploaded_file, "content_type", None)
    if content_type and content_type != "application/pdf":
        raise PdfValidationError("INVALID_MIME_TYPE", "PDF 파일만 업로드할 수 있습니다.")

    uploaded_file.seek(0)
    content = uploaded_file.read()
    uploaded_file.seek(0)

    if not content.startswith(PDF_SIGNATURE):
        raise PdfValidationError("INVALID_PDF_SIGNATURE", "올바른 PDF 파일이 아닙니다.")

    file_size = len(content)
    if file_size > MAX_FILE_SIZE_BYTES:
        raise PdfValidationError("FILE_TOO_LARGE", "파일 크기는 20MB를 초과할 수 없습니다.")

    try:
        doc = fitz.open(stream=content, filetype="pdf")
    except Exception as exc:
        raise PdfValidationError("CORRUPTED_PDF", "PDF 파일을 열 수 없습니다.") from exc

    try:
        page_count = doc.page_count
        is_encrypted = bool(doc.is_encrypted)

        if page_count > MAX_PAGE_COUNT:
            raise PdfValidationError(
                "TOO_MANY_PAGES", f"페이지 수는 {MAX_PAGE_COUNT}페이지를 초과할 수 없습니다."
            )

        if is_encrypted:
            raise PdfValidationError("ENCRYPTED_PDF", "암호화된 PDF는 업로드할 수 없습니다.")

        if page_count == 0 or _is_blank_document(doc):
            raise PdfValidationError("EMPTY_PDF", "빈 PDF 파일입니다.")
    finally:
        doc.close()

    file_hash = hashlib.sha256(content).hexdigest()

    return PdfFileInfo(
        file_size=file_size,
        file_hash=file_hash,
        page_count=page_count,
        is_encrypted=is_encrypted,
    )
