from django.test import SimpleTestCase

from apps.pdf_analysis.parsers.section_detector import SectionType, detect_sections, match_section_header
from apps.pdf_analysis.schemas.document import ExtractedBlock, ExtractedDocument


def _block(page, order, text):
    return ExtractedBlock(page=page, order=order, text=text, bbox=(0, 0, 10, 10), method="text")


class MatchSectionHeaderTests(SimpleTestCase):
    def test_matches_korean_variants(self):
        self.assertEqual(match_section_header("경력"), SectionType.CAREER)
        self.assertEqual(match_section_header("경력사항"), SectionType.CAREER)
        self.assertEqual(match_section_header("경력 사항"), SectionType.CAREER)
        self.assertEqual(match_section_header("학력"), SectionType.EDUCATION)
        self.assertEqual(match_section_header("프로젝트"), SectionType.PROJECT)
        self.assertEqual(match_section_header("자격증"), SectionType.CERTIFICATE)
        self.assertEqual(match_section_header("기술 스택"), SectionType.SKILL)
        self.assertEqual(match_section_header("어학"), SectionType.LANGUAGE)

    def test_matches_english_variants_case_insensitive(self):
        self.assertEqual(match_section_header("Work Experience"), SectionType.CAREER)
        self.assertEqual(match_section_header("education"), SectionType.EDUCATION)
        self.assertEqual(match_section_header("SKILLS"), SectionType.SKILL)
        self.assertEqual(match_section_header("Languages"), SectionType.LANGUAGE)

    def test_does_not_match_long_paragraph_containing_keyword(self):
        text = "저는 다양한 프로젝트 경험을 통해 실력을 쌓아온 개발자입니다"
        self.assertIsNone(match_section_header(text))

    def test_does_not_match_unrelated_text(self):
        self.assertIsNone(match_section_header("ABC주식회사 백엔드 개발자"))
        self.assertIsNone(match_section_header("2020.03 ~ 2022.05"))


class DetectSectionsTests(SimpleTestCase):
    def test_blocks_before_first_header_are_basic(self):
        document = ExtractedDocument(
            file_path="x.pdf",
            page_count=1,
            blocks=[
                _block(1, 0, "홍길동"),
                _block(1, 1, "email: hong@example.com"),
                _block(1, 2, "경력"),
                _block(1, 3, "ABC주식회사 백엔드 개발자"),
            ],
        )
        sections = detect_sections(document)
        self.assertEqual([b.text for b in sections[SectionType.BASIC]], ["홍길동", "email: hong@example.com"])
        self.assertEqual([b.text for b in sections[SectionType.CAREER]], ["ABC주식회사 백엔드 개발자"])

    def test_header_line_itself_is_excluded_from_content(self):
        document = ExtractedDocument(
            file_path="x.pdf",
            page_count=1,
            blocks=[_block(1, 0, "학력"), _block(1, 1, "한국대학교")],
        )
        sections = detect_sections(document)
        self.assertEqual(len(sections[SectionType.EDUCATION]), 1)
        self.assertEqual(sections[SectionType.EDUCATION][0].text, "한국대학교")

    def test_multiple_sections_split_correctly(self):
        document = ExtractedDocument(
            file_path="x.pdf",
            page_count=1,
            blocks=[
                _block(1, 0, "경력"),
                _block(1, 1, "회사 A"),
                _block(1, 2, "학력"),
                _block(1, 3, "학교 A"),
                _block(1, 4, "기술"),
                _block(1, 5, "Python"),
            ],
        )
        sections = detect_sections(document)
        self.assertEqual([b.text for b in sections[SectionType.CAREER]], ["회사 A"])
        self.assertEqual([b.text for b in sections[SectionType.EDUCATION]], ["학교 A"])
        self.assertEqual([b.text for b in sections[SectionType.SKILL]], ["Python"])

    def test_blocks_are_ordered_by_page_then_order(self):
        document = ExtractedDocument(
            file_path="x.pdf",
            page_count=2,
            blocks=[
                _block(2, 0, "second page line"),
                _block(1, 1, "first page second line"),
                _block(1, 0, "first page first line"),
            ],
        )
        sections = detect_sections(document)
        self.assertEqual(
            [b.text for b in sections[SectionType.BASIC]],
            ["first page first line", "first page second line", "second page line"],
        )
