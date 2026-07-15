from django.test import SimpleTestCase

from apps.dart.matching import match_company
from apps.dart.normalization import normalize_company_name
from apps.pdf_analysis.schemas.common import DartMatchStatus


class NormalizeCompanyNameTests(SimpleTestCase):
    def test_removes_common_suffixes(self):
        self.assertEqual(normalize_company_name("삼성전자주식회사"), "삼성전자")
        self.assertEqual(normalize_company_name("(주)한아티"), "한아티")
        self.assertEqual(normalize_company_name("㈜한아티"), "한아티")
        self.assertEqual(normalize_company_name("한아티유한회사"), "한아티")

    def test_removes_special_characters_and_spaces(self):
        self.assertEqual(normalize_company_name("ABC 상사(주)"), "abc상사")
        # 영문 법인 접미사(Co./Inc 등)는 SPEC.md 6장 제거 대상이 아니므로 유지된다.
        self.assertEqual(normalize_company_name("ABC-Trading Co."), "abctradingco")

    def test_lowercases_english(self):
        self.assertEqual(normalize_company_name("ABC Inc"), "abcinc")

    def test_empty_input(self):
        self.assertEqual(normalize_company_name(""), "")
        self.assertEqual(normalize_company_name(None), "")


def _corp(corp_code, corp_name, stock_code=""):
    return {
        "corp_code": corp_code,
        "corp_name": corp_name,
        "stock_code": stock_code,
        "normalized_name": normalize_company_name(corp_name),
    }


CORPORATIONS = [
    _corp("00126380", "삼성전자주식회사", "005930"),
    _corp("00164779", "한아티주식회사"),
    _corp("00401731", "ABC코리아"),
    _corp("00500001", "ABC상사주식회사"),
]


class MatchCompanyTests(SimpleTestCase):
    def test_normalized_exact_match_is_auto_matched(self):
        result = match_company("삼성전자주식회사", CORPORATIONS)
        self.assertEqual(result["status"], DartMatchStatus.MATCHED_AUTO)
        self.assertEqual(len(result["candidates"]), 1)
        self.assertEqual(result["candidates"][0]["corp_code"], "00126380")

    def test_variant_suffix_still_normalized_exact_match(self):
        # "삼성전자(주)" 정규화하면 "삼성전자" — 목록의 "삼성전자주식회사"와 동일
        result = match_company("삼성전자(주)", CORPORATIONS)
        self.assertEqual(result["status"], DartMatchStatus.MATCHED_AUTO)

    def test_substring_match_returns_multiple_candidates(self):
        result = match_company("ABC", CORPORATIONS)
        self.assertEqual(result["status"], DartMatchStatus.MULTIPLE_CANDIDATES)
        codes = {c["corp_code"] for c in result["candidates"]}
        self.assertEqual(codes, {"00401731", "00500001"})

    def test_no_match_returns_not_found(self):
        result = match_company("존재하지않는회사이름", CORPORATIONS)
        self.assertEqual(result["status"], DartMatchStatus.NOT_FOUND)
        self.assertEqual(result["candidates"], [])

    def test_short_name_exact_match_is_not_auto_connected(self):
        corporations = CORPORATIONS + [_corp("00999999", "가나")]
        result = match_company("가나", corporations)
        self.assertEqual(result["status"], DartMatchStatus.MULTIPLE_CANDIDATES)
        self.assertEqual(len(result["candidates"]), 1)

    def test_empty_query_returns_not_found(self):
        result = match_company("", CORPORATIONS)
        self.assertEqual(result["status"], DartMatchStatus.NOT_FOUND)

    def test_similarity_fallback_for_typo(self):
        corporations = [_corp("00700000", "네이버주식회사")]
        result = match_company("네이버", corporations)
        # "네이버"는 부분 문자열로도 매칭되므로 자동 매칭
        self.assertEqual(result["status"], DartMatchStatus.MATCHED_AUTO)
