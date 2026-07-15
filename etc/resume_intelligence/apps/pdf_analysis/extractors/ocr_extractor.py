"""pytesseract 기반 페이지 단위 OCR 추출기.

Tesseract가 시스템에 설치되어 있지 않으면 is_available()이 False를 반환한다
(pytesseract.get_tesseract_version() 호출을 1회만 시도해 결과를 캐시). 이
경우 HybridExtractor가 OCR을 건너뛰고 warnings에 안내를 남기는 graceful
degrade를 수행한다.
"""
import io

import fitz
import pytesseract
from PIL import Image

from ..schemas.document import ExtractedBlock

OCR_DPI = 200
OCR_LANGUAGES = "kor+eng"
OCR_FALLBACK_LANGUAGES = "eng"

_availability_cache: bool | None = None


class OcrExtractor:
    @staticmethod
    def is_available() -> bool:
        global _availability_cache
        if _availability_cache is None:
            try:
                pytesseract.get_tesseract_version()
                _availability_cache = True
            except Exception:
                _availability_cache = False
        return _availability_cache

    @staticmethod
    def reset_availability_cache() -> None:
        """테스트 전용: 캐시된 설치 여부 판정을 초기화한다."""
        global _availability_cache
        _availability_cache = None

    def extract_page(self, page: fitz.Page, page_number: int) -> list[ExtractedBlock]:
        pix = page.get_pixmap(dpi=OCR_DPI)
        image = Image.open(io.BytesIO(pix.tobytes("png")))
        try:
            text = pytesseract.image_to_string(image, lang=OCR_LANGUAGES)
        except pytesseract.TesseractError:
            # 한국어 언어팩 미설치 등으로 실패하면 영어만으로 재시도한다.
            text = pytesseract.image_to_string(image, lang=OCR_FALLBACK_LANGUAGES)
        text = text.strip()
        if not text:
            return []

        rect = page.rect
        return [
            ExtractedBlock(
                page=page_number,
                order=0,
                text=text,
                bbox=(0.0, 0.0, float(rect.width), float(rect.height)),
                method="ocr",
            )
        ]
