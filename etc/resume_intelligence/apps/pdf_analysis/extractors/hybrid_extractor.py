"""전체 페이지 텍스트 추출 → 품질 평가 → 낮은 품질 페이지만 OCR → 병합.

기본으로 사용되는 추출기. Tesseract 미설치 시 OCR을 건너뛰고 warnings에
OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED를 남긴다 (graceful degrade).
"""
import fitz

from ..schemas.document import ExtractedDocument
from .ocr_extractor import OcrExtractor
from .pymupdf_extractor import PyMuPdfExtractor
from .quality import assess_page_quality

OCR_UNAVAILABLE_WARNING = "OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED"


class HybridExtractor:
    def __init__(self, pymupdf_extractor=None, ocr_extractor=None):
        self._pymupdf_extractor = pymupdf_extractor or PyMuPdfExtractor()
        self._ocr_extractor = ocr_extractor or OcrExtractor()

    def extract(self, file_path: str) -> ExtractedDocument:
        document = self._pymupdf_extractor.extract(file_path)
        warnings = list(document.warnings)
        ocr_used = False

        doc = fitz.open(str(file_path))
        try:
            low_quality_pages = [
                page_index
                for page_index in range(doc.page_count)
                if assess_page_quality(doc[page_index], page_index + 1).needs_ocr
            ]

            if low_quality_pages:
                if not self._ocr_extractor.is_available():
                    warnings.append(OCR_UNAVAILABLE_WARNING)
                else:
                    ocr_blocks_by_page = {}
                    for page_index in low_quality_pages:
                        ocr_blocks = self._ocr_extractor.extract_page(doc[page_index], page_index + 1)
                        if ocr_blocks:
                            ocr_blocks_by_page[page_index + 1] = ocr_blocks

                    if ocr_blocks_by_page:
                        ocr_used = True
                        merged_blocks = [
                            b for b in document.blocks if b.page not in ocr_blocks_by_page
                        ]
                        for blocks in ocr_blocks_by_page.values():
                            merged_blocks.extend(blocks)
                        merged_blocks.sort(key=lambda b: (b.page, b.order))
                        document = document.model_copy(update={"blocks": merged_blocks})
        finally:
            doc.close()

        return document.model_copy(update={"ocr_used": ocr_used, "warnings": warnings})
