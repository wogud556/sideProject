"""여러 스키마에서 공유하는 공통 타입."""
from enum import Enum
from typing import Generic, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class ExtractedField(BaseModel, Generic[T]):
    """규칙/LLM 파서가 추출한 값 하나를 감싸는 공통 래퍼.

    SPEC.md 2장: "각 추출값에 confidence, source_page, source_text,
    review_required 동반".
    """

    value: T | None = None
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class DartMatchStatus(str, Enum):
    NOT_SEARCHED = "NOT_SEARCHED"
    MATCHED_AUTO = "MATCHED_AUTO"
    MATCHED_MANUAL = "MATCHED_MANUAL"
    MULTIPLE_CANDIDATES = "MULTIPLE_CANDIDATES"
    NOT_FOUND = "NOT_FOUND"
    ERROR = "ERROR"
