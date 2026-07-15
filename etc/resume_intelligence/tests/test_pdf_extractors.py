import shutil
import tempfile
from pathlib import Path

from django.test import SimpleTestCase

from apps.pdf_analysis.extractors.pymupdf_extractor import PyMuPdfExtractor
from apps.pdf_analysis.schemas.document import ExtractedDocument

from .helpers import build_multi_page_text_pdf, build_text_resume_pdf


class PyMuPdfExtractorTests(SimpleTestCase):
    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)
        self.extractor = PyMuPdfExtractor()

    def _write(self, content: bytes, name="resume.pdf") -> Path:
        path = self.tmp_dir / name
        path.write_bytes(content)
        return path

    def test_extract_returns_extracted_document(self):
        path = self._write(build_text_resume_pdf())
        document = self.extractor.extract(str(path))
        self.assertIsInstance(document, ExtractedDocument)
        self.assertEqual(document.page_count, 1)
        self.assertFalse(document.ocr_used)
        self.assertEqual(document.warnings, [])

    def test_extract_finds_expected_text_blocks(self):
        path = self._write(build_text_resume_pdf())
        document = self.extractor.extract(str(path))
        all_text = " ".join(b.text for b in document.blocks)
        self.assertIn("홍길동", all_text)
        self.assertIn("hong.gildong@example.com", all_text)
        self.assertIn("경력", all_text)

    def test_blocks_have_method_text_and_valid_bbox(self):
        path = self._write(build_text_resume_pdf())
        document = self.extractor.extract(str(path))
        self.assertGreater(len(document.blocks), 0)
        for block in document.blocks:
            self.assertEqual(block.method, "text")
            x0, y0, x1, y1 = block.bbox
            self.assertLessEqual(x0, x1)
            self.assertLessEqual(y0, y1)

    def test_blocks_ordered_per_page(self):
        path = self._write(build_text_resume_pdf())
        document = self.extractor.extract(str(path))
        orders = [b.order for b in document.blocks if b.page == 1]
        self.assertEqual(orders, sorted(orders))

    def test_multi_page_document_assigns_correct_page_numbers(self):
        content = build_multi_page_text_pdf([["Page One"], ["Page Two"], ["Page Three"]])
        path = self._write(content)
        document = self.extractor.extract(str(path))
        self.assertEqual(document.page_count, 3)
        pages_present = {b.page for b in document.blocks}
        self.assertEqual(pages_present, {1, 2, 3})
