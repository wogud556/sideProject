import shutil
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import fitz
from django.test import SimpleTestCase

from apps.pdf_analysis.extractors.hybrid_extractor import (
    OCR_UNAVAILABLE_WARNING,
    HybridExtractor,
)
from apps.pdf_analysis.extractors.ocr_extractor import OcrExtractor
from apps.pdf_analysis.schemas.document import ExtractedBlock

from .helpers import build_image_only_pdf, build_text_resume_pdf


class OcrAvailabilityCacheTests(SimpleTestCase):
    def setUp(self):
        OcrExtractor.reset_availability_cache()
        self.addCleanup(OcrExtractor.reset_availability_cache)

    def test_is_available_true_and_cached_after_first_call(self):
        target = "apps.pdf_analysis.extractors.ocr_extractor.pytesseract.get_tesseract_version"
        with patch(target) as mock_version:
            mock_version.return_value = "5.5.2"
            self.assertTrue(OcrExtractor.is_available())
            self.assertTrue(OcrExtractor.is_available())
            self.assertEqual(mock_version.call_count, 1)

    def test_is_available_false_and_cached_when_tesseract_missing(self):
        target = "apps.pdf_analysis.extractors.ocr_extractor.pytesseract.get_tesseract_version"
        with patch(target) as mock_version:
            mock_version.side_effect = EnvironmentError("tesseract is not installed")
            self.assertFalse(OcrExtractor.is_available())
            self.assertFalse(OcrExtractor.is_available())
            self.assertEqual(mock_version.call_count, 1)


@unittest.skipUnless(
    OcrExtractor.is_available(), "Tesseract가 설치되어 있지 않아 실제 OCR 경로 테스트를 건너뜁니다."
)
class RealOcrExtractionTests(SimpleTestCase):
    """실제 설치된 Tesseract를 사용해 OCR 경로 자체를 검증한다."""

    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_extract_page_recognizes_text_from_image_only_page(self):
        content = build_image_only_pdf(["HONG GILDONG", "PYTHON DEVELOPER"])
        doc = fitz.open(stream=content, filetype="pdf")
        try:
            blocks = OcrExtractor().extract_page(doc[0], 1)
        finally:
            doc.close()
        self.assertEqual(len(blocks), 1)
        self.assertEqual(blocks[0].method, "ocr")
        self.assertIn("HONG", blocks[0].text.upper())

    def test_hybrid_extractor_runs_ocr_for_image_only_pdf(self):
        path = self.tmp_dir / "scanned.pdf"
        path.write_bytes(build_image_only_pdf(["HONG GILDONG", "PYTHON DEVELOPER"]))
        document = HybridExtractor().extract(str(path))

        self.assertTrue(document.ocr_used)
        self.assertNotIn(OCR_UNAVAILABLE_WARNING, document.warnings)
        ocr_blocks = [b for b in document.blocks if b.method == "ocr"]
        self.assertEqual(len(ocr_blocks), 1)
        self.assertIn("HONG", ocr_blocks[0].text.upper())

    def test_hybrid_extractor_does_not_run_ocr_for_clean_text_pdf(self):
        path = self.tmp_dir / "resume.pdf"
        path.write_bytes(build_text_resume_pdf())
        document = HybridExtractor().extract(str(path))

        self.assertFalse(document.ocr_used)
        self.assertEqual(document.warnings, [])


class _UnavailableOcrExtractor:
    def is_available(self):
        return False

    def extract_page(self, page, page_number):
        raise AssertionError("OCR이 불가능할 때는 extract_page가 호출되면 안 된다")


class _StubOcrExtractor:
    """실제 Tesseract 정확도에 의존하지 않고 병합 로직만 검증하기 위한 더블."""

    def is_available(self):
        return True

    def extract_page(self, page, page_number):
        return [
            ExtractedBlock(
                page=page_number,
                order=0,
                text="스텁 OCR 텍스트",
                bbox=(0.0, 0.0, 100.0, 100.0),
                method="ocr",
            )
        ]


class HybridExtractorGracefulDegradeTests(SimpleTestCase):
    """OcrExtractor를 더블로 주입해 Tesseract 설치 여부와 무관하게 병합/경고 로직을 검증한다."""

    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_skips_ocr_and_adds_warning_when_tesseract_unavailable(self):
        path = self.tmp_dir / "scanned.pdf"
        path.write_bytes(build_image_only_pdf())
        extractor = HybridExtractor(ocr_extractor=_UnavailableOcrExtractor())

        document = extractor.extract(str(path))

        self.assertFalse(document.ocr_used)
        self.assertIn(OCR_UNAVAILABLE_WARNING, document.warnings)

    def test_does_not_call_ocr_for_clean_text_pdf_even_if_available(self):
        path = self.tmp_dir / "resume.pdf"
        path.write_bytes(build_text_resume_pdf())
        extractor = HybridExtractor(ocr_extractor=_UnavailableOcrExtractor())

        document = extractor.extract(str(path))

        self.assertFalse(document.ocr_used)
        self.assertEqual(document.warnings, [])

    def test_merges_ocr_blocks_for_low_quality_page(self):
        path = self.tmp_dir / "scanned.pdf"
        path.write_bytes(build_image_only_pdf())
        extractor = HybridExtractor(ocr_extractor=_StubOcrExtractor())

        document = extractor.extract(str(path))

        self.assertTrue(document.ocr_used)
        self.assertEqual(len(document.blocks), 1)
        self.assertEqual(document.blocks[0].text, "스텁 OCR 텍스트")
        self.assertEqual(document.blocks[0].method, "ocr")
