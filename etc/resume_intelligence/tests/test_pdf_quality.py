import fitz
from django.test import SimpleTestCase

from apps.pdf_analysis.extractors.quality import assess_page_quality
from apps.pdf_analysis.schemas.document import PageQuality

from .helpers import build_image_only_pdf, build_text_resume_pdf


class AssessPageQualityTests(SimpleTestCase):
    def test_text_page_does_not_need_ocr(self):
        doc = fitz.open(stream=build_text_resume_pdf(), filetype="pdf")
        try:
            quality = assess_page_quality(doc[0], 1)
        finally:
            doc.close()
        self.assertIsInstance(quality, PageQuality)
        self.assertFalse(quality.needs_ocr)
        self.assertGreater(quality.char_count, 20)

    def test_image_only_page_needs_ocr(self):
        doc = fitz.open(stream=build_image_only_pdf(), filetype="pdf")
        try:
            quality = assess_page_quality(doc[0], 1)
        finally:
            doc.close()
        self.assertTrue(quality.needs_ocr)
        self.assertEqual(quality.char_count, 0)
        self.assertGreater(quality.image_area_ratio, 0.5)

    def test_page_number_is_recorded(self):
        doc = fitz.open(stream=build_text_resume_pdf(), filetype="pdf")
        try:
            quality = assess_page_quality(doc[0], 5)
        finally:
            doc.close()
        self.assertEqual(quality.page, 5)
