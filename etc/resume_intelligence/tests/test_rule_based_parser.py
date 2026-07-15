from django.test import SimpleTestCase

from apps.pdf_analysis.parsers.base import ResumeParseResult
from apps.pdf_analysis.parsers.llm_parser import LlmResumeParser
from apps.pdf_analysis.parsers.rule_based_parser import RuleBasedResumeParser
from apps.pdf_analysis.schemas.document import ExtractedBlock, ExtractedDocument
from apps.pdf_analysis.schemas.profile import ResumeProfile


def _block(page, order, text):
    return ExtractedBlock(page=page, order=order, text=text, bbox=(0, 0, 10, 10), method="text")


def _sample_document() -> ExtractedDocument:
    lines = [
        "홍길동",
        "email: hong.gildong@example.com",
        "phone: 010-1234-5678",
        "경력",
        "ABC주식회사 백엔드 개발자",
        "2020.03 ~ 2022.05",
        "- 결제 시스템 개발",
        "(주)한아티 서버 개발자",
        "2022.06 ~ 재직중",
        "- 이력서 분석 서비스 개발",
        "학력",
        "한국대학교 컴퓨터공학과",
        "2016.03 ~ 2020.02",
        "프로젝트",
        "이력서 분석기 개발",
        "2023.01 ~ 2023.06",
        "자격증",
        "정보처리기사 한국산업인력공단 2021.11",
        "기술",
        "Python, Django, PostgreSQL",
        "어학",
        "TOEIC 900 2020.05",
    ]
    blocks = [_block(1, i, text) for i, text in enumerate(lines)]
    return ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])


class RuleBasedResumeParserTests(SimpleTestCase):
    def setUp(self):
        self.parser = RuleBasedResumeParser()
        self.result = self.parser.parse(_sample_document())

    def test_returns_resume_parse_result_with_profile(self):
        self.assertIsInstance(self.result, ResumeParseResult)
        self.assertIsInstance(self.result.profile, ResumeProfile)

    def test_extracts_basic_info(self):
        basic = self.result.profile.basic
        self.assertEqual(basic.name.value, "홍길동")
        self.assertEqual(basic.email.value, "hong.gildong@example.com")
        self.assertEqual(basic.phone.value, "010-1234-5678")
        self.assertGreaterEqual(basic.email.confidence, 0.7)
        self.assertFalse(basic.email.review_required)

    def test_extracts_two_careers_with_correct_dates(self):
        careers = self.result.profile.careers
        self.assertEqual(len(careers), 2)

        first, second = careers
        self.assertIn("ABC주식회사", first.company_name_raw)
        self.assertEqual(first.start_date, "2020-03")
        self.assertEqual(first.end_date, "2022-05")
        self.assertFalse(first.is_current)
        self.assertIn("- 결제 시스템 개발", first.responsibilities)

        self.assertIn("한아티", second.company_name_raw)
        self.assertEqual(second.start_date, "2022-06")
        self.assertIsNone(second.end_date)
        self.assertTrue(second.is_current)

    def test_career_sort_order_is_sequential(self):
        careers = self.result.profile.careers
        self.assertEqual([c.sort_order for c in careers], [0, 1])

    def test_extracts_education(self):
        educations = self.result.profile.educations
        self.assertEqual(len(educations), 1)
        self.assertIn("한국대학교", educations[0].school_name)
        self.assertEqual(educations[0].start_date, "2016-03")
        self.assertEqual(educations[0].end_date, "2020-02")

    def test_extracts_project(self):
        projects = self.result.profile.projects
        self.assertEqual(len(projects), 1)
        self.assertIn("이력서 분석기", projects[0].project_name)
        self.assertEqual(projects[0].start_date, "2023-01")
        self.assertEqual(projects[0].end_date, "2023-06")

    def test_extracts_certificate(self):
        certificates = self.result.profile.certificates
        self.assertEqual(len(certificates), 1)
        self.assertEqual(certificates[0].certificate_name, "정보처리기사")
        self.assertEqual(certificates[0].issuer, "한국산업인력공단")
        self.assertEqual(certificates[0].acquired_date, "2021-11")

    def test_extracts_skills_split_by_comma(self):
        skills = self.result.profile.skills
        names = [s.name for s in skills]
        self.assertEqual(names, ["Python", "Django", "PostgreSQL"])

    def test_extracts_language(self):
        languages = self.result.profile.languages
        self.assertEqual(len(languages), 1)
        self.assertEqual(languages[0].test_name, "TOEIC")
        self.assertEqual(languages[0].score, "900")
        self.assertEqual(languages[0].acquired_date, "2020-05")
        self.assertEqual(languages[0].language, "영어")

    def test_all_list_records_carry_confidence_and_source(self):
        for career in self.result.profile.careers:
            self.assertGreater(career.confidence, 0)
            self.assertEqual(career.source_page, 1)
            self.assertIsNotNone(career.source_text)


class RuleBasedResumeParserEmptyDocumentTests(SimpleTestCase):
    def test_empty_document_returns_empty_profile_without_error(self):
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=[], ocr_used=False, warnings=[])
        result = RuleBasedResumeParser().parse(document)

        self.assertEqual(result.profile.careers, [])
        self.assertEqual(result.profile.educations, [])
        self.assertIsNone(result.profile.basic.name.value)


class LlmResumeParserStubTests(SimpleTestCase):
    def test_parse_raises_not_implemented(self):
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=[], ocr_used=False, warnings=[])
        with self.assertRaises(NotImplementedError):
            LlmResumeParser().parse(document)


class CareerPeriodValidationHookupTests(SimpleTestCase):
    """MED-1 회귀 테스트: validate_career_period/detect_overlapping_periods가
    RuleBasedResumeParser.parse()에 실제로 연결되어 warnings/review_required에
    반영되는지 확인한다(파이프라인에 연결되지 않은 dead code가 아님을 검증)."""

    def test_reversed_dates_produce_warning_and_review_required(self):
        lines = [
            "경력",
            "ABC주식회사",
            "2023.01 ~ 2020.01",  # 입사일이 퇴사일보다 늦음(역전)
            "- 업무",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)

        self.assertTrue(any("늦습니다" in w for w in result.warnings))
        self.assertEqual(len(result.profile.careers), 1)
        self.assertTrue(result.profile.careers[0].review_required)

    def test_overlapping_careers_produce_warning(self):
        lines = [
            "경력",
            "A회사",
            "2018.01 ~ 2020.06",
            "B회사",
            "2020.01 ~ 2021.12",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)

        self.assertTrue(any("겹칩니다" in w for w in result.warnings))

    def test_valid_non_overlapping_careers_produce_no_period_warnings(self):
        result = RuleBasedResumeParser().parse(_sample_document())
        self.assertFalse(any("겹칩니다" in w or "늦습니다" in w for w in result.warnings))
        for career in result.profile.careers:
            self.assertFalse(career.review_required)
