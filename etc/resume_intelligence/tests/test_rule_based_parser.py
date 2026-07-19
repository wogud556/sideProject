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
        """회귀 확인: 회사→회사가 중간에 업무 불릿 없이 바로 이어지는 경우
        (sub_projects 그룹핑 도입 이전과 동일하게) 여전히 careers 2건 +
        겹침 경고를 낸다."""
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

        self.assertEqual(len(result.profile.careers), 2)
        self.assertTrue(any("겹칩니다" in w for w in result.warnings))

    def test_valid_non_overlapping_careers_produce_no_period_warnings(self):
        result = RuleBasedResumeParser().parse(_sample_document())
        self.assertFalse(any("겹칩니다" in w or "늦습니다" in w for w in result.warnings))
        for career in result.profile.careers:
            self.assertFalse(career.review_required)


class CareerSubProjectGroupingTests(SimpleTestCase):
    """경력기술서 본문 안의 프로젝트 서브엔트리(sub_projects) 그룹핑 검증.

    _group_career_entries가 "헤더+다음줄날짜" 패턴을 만났을 때, 이미 쌓인
    detail_blocks가 있고 헤더에 회사 접미사가 없으면 sub_projects로,
    그렇지 않으면 새 Career 경계로 판단하는 로직을 검증한다.
    """

    def test_suffix_less_project_subentry_is_absorbed_into_sub_projects(self):
        """정상 케이스: 회사 헤더+기간, 업무 불릿 1개 이상 뒤에 접미사 없는
        프로젝트 헤더+기간+상세줄이 오면 sub_projects로 파싱되고, 회사간
        기간 겹침 오탐도 발생하지 않는다."""
        lines = [
            "경력",
            "ABC주식회사 백엔드 개발자",
            "2020.03 ~ 2022.05",
            "- 결제 시스템 개발",
            "이력서 매칭 시스템 구축",
            "2021.01 ~ 2021.06",
            "- 매칭 알고리즘 개선",
            "- 성능 최적화",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)
        careers = result.profile.careers

        self.assertEqual(len(careers), 1)
        career = careers[0]
        self.assertIn("- 결제 시스템 개발", career.responsibilities)

        self.assertEqual(len(career.sub_projects), 1)
        sub_project = career.sub_projects[0]
        self.assertEqual(sub_project.project_name, "이력서 매칭 시스템 구축")
        self.assertEqual(sub_project.start_date, "2021-01")
        self.assertEqual(sub_project.end_date, "2021-06")
        self.assertEqual(sub_project.achievements, ["- 매칭 알고리즘 개선", "- 성능 최적화"])
        self.assertTrue(sub_project.review_required)
        self.assertEqual(sub_project.confidence, 0.4)

        self.assertFalse(any("겹칩니다" in w for w in result.warnings))

    def test_second_company_with_suffix_after_sub_project_is_parsed_independently(self):
        """접미사 있는 두 번째 회사 케이스: 첫 회사 안에 프로젝트 서브엔트리가
        있더라도, 그 다음에 회사 접미사가 붙은 진짜 두 번째 회사가 오면
        sub_projects로 흡수되지 않고 독립된 Career로 분리된다."""
        lines = [
            "경력",
            "ABC주식회사 백엔드 개발자",
            "2020.03 ~ 2022.05",
            "- 결제 시스템 개발",
            "이력서 매칭 시스템 구축",
            "2021.01 ~ 2021.06",
            "- 매칭 알고리즘 개선",
            "XYZ주식회사 프론트엔드 개발자",
            "2022.07 ~ 2023.12",
            "- UI 개발",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)
        careers = result.profile.careers

        self.assertEqual(len(careers), 2)
        first, second = careers

        self.assertIn("ABC주식회사", first.company_name_raw)
        self.assertEqual(len(first.sub_projects), 1)
        self.assertEqual(first.sub_projects[0].project_name, "이력서 매칭 시스템 구축")

        self.assertIn("XYZ주식회사", second.company_name_raw)
        self.assertEqual(second.start_date, "2022-07")
        self.assertEqual(second.end_date, "2023-12")
        self.assertEqual(second.sub_projects, [])
        self.assertIn("- UI 개발", second.responsibilities)

    def test_known_limitation_case_a_subheader_immediately_after_date_misclassified_as_new_company(self):
        """알려진 한계(케이스 A): 회사 날짜 라인 바로 다음 줄이 (일반 업무
        텍스트 없이) 곧바로 프로젝트 서브헤더+날짜인 경우, detail_blocks가
        비어있어 새 Career로 오분류된다. 이 테스트는 그 현재 동작을 그대로
        문서화하는 회귀 테스트이며, 로직 수정을 요구하는 것이 아니다."""
        lines = [
            "경력",
            "ABC주식회사 백엔드 개발자",
            "2020.03 ~ 2022.05",
            "이력서 매칭 시스템 구축",
            "2021.01 ~ 2021.06",
            "- 매칭 알고리즘 개선",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)
        careers = result.profile.careers

        # 의도치 않게 프로젝트 서브헤더가 새로운 Career로 분리된다.
        self.assertEqual(len(careers), 2)
        first, second = careers
        self.assertIn("ABC주식회사", first.company_name_raw)
        self.assertEqual(first.sub_projects, [])
        self.assertEqual(first.responsibilities, [])

        self.assertEqual(second.company_name_raw, "이력서 매칭 시스템 구축")
        self.assertEqual(second.sub_projects, [])
        self.assertIn("- 매칭 알고리즘 개선", second.responsibilities)

        # 두 Career의 기간이 겹치므로, 원래 있던 "경력 기간이 겹칩니다" 오탐이
        # 이 케이스에서는 그대로 재현된다.
        self.assertTrue(any("겹칩니다" in w for w in result.warnings))

    def test_known_limitation_case_b_suffix_less_second_company_absorbed_as_sub_project(self):
        """알려진 한계(케이스 B): 회사 접미사가 없는 진짜 두 번째 직장이 첫
        회사의 일반 업무 텍스트 뒤에 나오면, 새 Career가 아니라 첫 회사의
        sub_projects로 잘못 흡수된다. 이 테스트는 그 현재 동작을 그대로
        문서화하는 회귀 테스트이며, 로직 수정을 요구하는 것이 아니다."""
        lines = [
            "경력",
            "ABC주식회사 백엔드 개발자",
            "2020.03 ~ 2022.05",
            "- 결제 시스템 개발",
            "카카오 백엔드 개발자",
            "2022.06 ~ 2023.12",
            "- 새로운 업무",
        ]
        blocks = [_block(1, i, text) for i, text in enumerate(lines)]
        document = ExtractedDocument(file_path="x.pdf", page_count=1, blocks=blocks, ocr_used=False, warnings=[])

        result = RuleBasedResumeParser().parse(document)
        careers = result.profile.careers

        # 진짜 별도 경력("카카오")이 최상위 careers 리스트에서 사라지고
        # 첫 회사의 sub_projects로 흡수된다.
        self.assertEqual(len(careers), 1)
        career = careers[0]
        self.assertIn("ABC주식회사", career.company_name_raw)
        self.assertEqual(len(career.sub_projects), 1)
        self.assertEqual(career.sub_projects[0].project_name, "카카오 백엔드 개발자")
        self.assertFalse(any(c.company_name_raw == "카카오 백엔드 개발자" for c in careers))
