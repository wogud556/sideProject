from django.test import SimpleTestCase

from apps.pdf_analysis.parsers.field_extractors import (
    contains_company_suffix,
    extract_date_range,
    extract_emails,
    extract_name_from_label,
    extract_phones,
    extract_single_date,
    infer_language_from_test_name,
)


class ExtractEmailsTests(SimpleTestCase):
    def test_extracts_email_from_labeled_line(self):
        self.assertEqual(extract_emails("email: hong.gildong@example.com"), ["hong.gildong@example.com"])

    def test_returns_empty_list_when_no_email(self):
        self.assertEqual(extract_emails("phone: 010-1234-5678"), [])

    def test_extracts_multiple_emails(self):
        text = "primary: a@example.com secondary: b@example.co.kr"
        self.assertEqual(extract_emails(text), ["a@example.com", "b@example.co.kr"])


class ExtractPhonesTests(SimpleTestCase):
    def test_extracts_mobile_phone(self):
        self.assertEqual(extract_phones("phone: 010-1234-5678"), ["010-1234-5678"])

    def test_extracts_phone_without_dashes(self):
        self.assertEqual(extract_phones("연락처 01012345678"), ["01012345678"])

    def test_returns_empty_when_no_phone(self):
        self.assertEqual(extract_phones("email: hong@example.com"), [])


class ExtractNameFromLabelTests(SimpleTestCase):
    def test_matches_korean_label(self):
        self.assertEqual(extract_name_from_label("성명: 홍길동"), "홍길동")
        self.assertEqual(extract_name_from_label("이름 : 홍길동"), "홍길동")

    def test_matches_english_label(self):
        self.assertEqual(extract_name_from_label("Name: Hong Gildong"), "Hong Gildong")

    def test_returns_none_when_no_label(self):
        self.assertIsNone(extract_name_from_label("홍길동"))


class ExtractDateRangeTests(SimpleTestCase):
    def test_dot_separated_range(self):
        self.assertEqual(extract_date_range("2020.03 ~ 2022.05"), ("2020-03", "2022-05", False))

    def test_dash_separated_range(self):
        self.assertEqual(extract_date_range("2020-03 ~ 2022-05"), ("2020-03", "2022-05", False))

    def test_korean_year_month_format(self):
        self.assertEqual(extract_date_range("2020년 3월 ~ 2022년 5월"), ("2020-03", "2022-05", False))

    def test_current_job_open_ended(self):
        self.assertEqual(extract_date_range("2022.06 ~ 재직중"), ("2022-06", None, True))

    def test_current_job_present_keyword(self):
        self.assertEqual(extract_date_range("2022.06 ~ Present"), ("2022-06", None, True))

    def test_single_date_only(self):
        self.assertEqual(extract_date_range("2021.11"), ("2021-11", None, False))

    def test_no_date_returns_none(self):
        self.assertIsNone(extract_date_range("결제 시스템 개발"))


class ExtractSingleDateTests(SimpleTestCase):
    def test_extracts_first_date(self):
        self.assertEqual(extract_single_date("정보처리기사 한국산업인력공단 2021.11"), "2021-11")

    def test_returns_none_when_absent(self):
        self.assertIsNone(extract_single_date("정보처리기사"))


class CompanySuffixTests(SimpleTestCase):
    def test_detects_known_suffixes(self):
        for text in ["ABC주식회사", "(주)한아티", "㈜한아티", "Example Inc.", "Example Co., Ltd"]:
            self.assertTrue(contains_company_suffix(text), text)

    def test_no_suffix(self):
        self.assertFalse(contains_company_suffix("한국대학교"))


class InferLanguageTests(SimpleTestCase):
    def test_known_test_names(self):
        self.assertEqual(infer_language_from_test_name("TOEIC"), "영어")
        self.assertEqual(infer_language_from_test_name("jlpt"), "일본어")

    def test_unknown_test_name_returns_none(self):
        self.assertIsNone(infer_language_from_test_name("UNKNOWN_TEST"))

    def test_none_input_returns_none(self):
        self.assertIsNone(infer_language_from_test_name(None))
