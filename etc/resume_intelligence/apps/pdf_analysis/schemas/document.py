"""PDF 추출/업로드 문서 관련 스키마."""
from enum import Enum
from typing import Literal

from pydantic import BaseModel, Field


class ExtractedBlock(BaseModel):
    """페이지 내 텍스트 블록 하나."""

    page: int
    order: int
    text: str
    bbox: tuple[float, float, float, float]
    method: Literal["text", "ocr"] = "text"


class ExtractedDocument(BaseModel):
    """추출기(PdfExtractor)의 출력."""

    file_path: str
    page_count: int
    blocks: list[ExtractedBlock] = Field(default_factory=list)
    ocr_used: bool = False
    warnings: list[str] = Field(default_factory=list)


class PageQuality(BaseModel):
    """페이지 텍스트 품질 평가 결과 (OCR 필요 여부 판단용)."""

    page: int
    char_count: int
    broken_char_ratio: float
    image_area_ratio: float
    needs_ocr: bool


class AnalysisStatus(str, Enum):
    UPLOADED = "UPLOADED"
    EXTRACTING = "EXTRACTING"
    OCR_PROCESSING = "OCR_PROCESSING"
    STRUCTURING = "STRUCTURING"
    COMPLETED = "COMPLETED"
    COMPLETED_WITH_WARNINGS = "COMPLETED_WITH_WARNINGS"
    FAILED = "FAILED"


class DocumentRecord(BaseModel):
    """document.json에 저장되는 업로드 메타 + 분석 상태."""

    id: str
    user_id: str
    original_filename: str
    file_size: int
    file_hash: str
    page_count: int
    is_encrypted: bool = False
    analysis_status: AnalysisStatus = AnalysisStatus.UPLOADED
    ocr_used: bool = False
    uploaded_at: str
    analysis_completed_at: str | None = None
    error_code: str | None = None
    error_message: str | None = None
    warnings: list[str] = Field(default_factory=list)
