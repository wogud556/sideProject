import shutil
import tempfile
from pathlib import Path

from django.test import SimpleTestCase

from apps.pdf_analysis.parsers.base import ResumeParseResult
from apps.pdf_analysis.pipeline import PipelineResult, run_pipeline
from apps.pdf_analysis.schemas.document import AnalysisStatus, ExtractedDocument
from apps.pdf_analysis.schemas.profile import ResumeProfile

from .helpers import build_image_only_pdf, build_text_resume_pdf


class RunPipelineWithTextPdfTests(SimpleTestCase):
    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_full_pipeline_on_text_resume(self):
        path = self.tmp_dir / "resume.pdf"
        path.write_bytes(build_text_resume_pdf())

        result = run_pipeline(str(path))

        self.assertIsInstance(result, PipelineResult)
        self.assertGreater(len(result.blocks), 0)
        self.assertFalse(result.ocr_used)
        self.assertEqual(result.status, AnalysisStatus.COMPLETED)
        self.assertEqual(result.profile.basic.name.value, "홍길동")
        self.assertEqual(result.profile.basic.email.value, "hong.gildong@example.com")
        self.assertEqual(len(result.profile.careers), 2)


class RunPipelineExcludesSelfIntroTests(SimpleTestCase):
    """자기소개서 섹션은 파싱 입력과 결과 블록 모두에서 제외돼야 한다."""

    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_self_intro_section_is_excluded_from_result(self):
        path = self.tmp_dir / "with_self_intro.pdf"
        path.write_bytes(
            build_text_resume_pdf(
                [
                    "홍길동",
                    "email: hong@example.com",
                    "경력",
                    "ABC주식회사 백엔드 개발자",
                    "2020.03 ~ 2022.05",
                    "- 결제 시스템 개발",
                    "자기소개서",
                    "저는 성실한 개발자입니다.",
                    "어려서부터 컴퓨터를 좋아했습니다.",
                ]
            )
        )

        result = run_pipeline(str(path))

        texts = [b.text for b in result.blocks]
        self.assertTrue(any("ABC주식회사" in t for t in texts))
        self.assertFalse(any("자기소개서" in t for t in texts))
        self.assertFalse(any("성실한 개발자" in t for t in texts))
        self.assertFalse(any("컴퓨터를 좋아했습니다" in t for t in texts))
        self.assertEqual(len(result.profile.careers), 1)


class RunPipelineWithMalformedContactInfoTests(SimpleTestCase):
    """경계: 이메일/전화 형식이 이상한 이력서도 파이프라인이 죽지 않고
    완료돼야 한다(값을 잘못 추출하지 않고 None으로 남기는 것이 안전한 동작)."""

    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_pipeline_completes_without_crash_for_broken_contact_lines(self):
        path = self.tmp_dir / "broken_contact.pdf"
        path.write_bytes(
            build_text_resume_pdf(
                [
                    "홍길동",
                    "email hong.gildong (at) example dot com",  # @ 기호 없음
                    "phone 공일공-일이삼사-오육칠팔",  # 숫자가 아님
                ]
            )
        )

        result = run_pipeline(str(path))

        self.assertEqual(result.status, AnalysisStatus.COMPLETED)
        self.assertIsNone(result.profile.basic.email.value)
        self.assertIsNone(result.profile.basic.phone.value)


class RunPipelineWithImageOnlyPdfTests(SimpleTestCase):
    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_pipeline_falls_back_to_ocr_for_scanned_page(self):
        path = self.tmp_dir / "scanned.pdf"
        path.write_bytes(
            build_image_only_pdf(["HONG GILDONG", "email: hong@example.com", "phone: 010-1234-5678"])
        )

        result = run_pipeline(str(path))

        self.assertTrue(result.ocr_used)
        self.assertEqual(result.profile.basic.email.value, "hong@example.com")
        self.assertEqual(result.profile.basic.phone.value, "010-1234-5678")


class _StubExtractor:
    def __init__(self, document: ExtractedDocument):
        self._document = document

    def extract(self, file_path: str) -> ExtractedDocument:
        return self._document


class _StubParser:
    def __init__(self, profile: ResumeProfile, warnings=None):
        self._profile = profile
        self._warnings = warnings or []

    def parse(self, document: ExtractedDocument) -> ResumeParseResult:
        return ResumeParseResult(profile=self._profile, warnings=self._warnings)


class RunPipelineDependencyInjectionTests(SimpleTestCase):
    def test_uses_injected_extractor_and_parser(self):
        document = ExtractedDocument(file_path="dummy.pdf", page_count=1, blocks=[], ocr_used=False, warnings=[])
        profile = ResumeProfile()
        extractor = _StubExtractor(document)
        parser = _StubParser(profile)

        result = run_pipeline("dummy.pdf", extractor=extractor, parser=parser)

        self.assertIs(result.profile, profile)
        self.assertEqual(result.status, AnalysisStatus.COMPLETED)

    def test_extractor_warnings_result_in_completed_with_warnings(self):
        document = ExtractedDocument(
            file_path="dummy.pdf",
            page_count=1,
            blocks=[],
            ocr_used=False,
            warnings=["OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED"],
        )
        extractor = _StubExtractor(document)
        parser = _StubParser(ResumeProfile())

        result = run_pipeline("dummy.pdf", extractor=extractor, parser=parser)

        self.assertEqual(result.status, AnalysisStatus.COMPLETED_WITH_WARNINGS)
        self.assertIn("OCR_UNAVAILABLE_TESSERACT_NOT_INSTALLED", result.warnings)

    def test_parser_warnings_result_in_completed_with_warnings(self):
        document = ExtractedDocument(file_path="dummy.pdf", page_count=1, blocks=[], ocr_used=False, warnings=[])
        extractor = _StubExtractor(document)
        parser = _StubParser(ResumeProfile(), warnings=["일부 필드를 추출하지 못했습니다."])

        result = run_pipeline("dummy.pdf", extractor=extractor, parser=parser)

        self.assertEqual(result.status, AnalysisStatus.COMPLETED_WITH_WARNINGS)
        self.assertIn("일부 필드를 추출하지 못했습니다.", result.warnings)
