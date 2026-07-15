from django.test import SimpleTestCase

from apps.dart.account_mapping import CANONICAL_ACCOUNTS, map_account_name


class MapAccountNameTests(SimpleTestCase):
    def test_maps_known_variants_to_canonical_revenue(self):
        self.assertEqual(map_account_name("매출액"), "매출액")
        self.assertEqual(map_account_name("영업수익"), "매출액")
        self.assertEqual(map_account_name("수익(매출액)"), "매출액")

    def test_maps_known_variants_to_canonical_operating_profit(self):
        self.assertEqual(map_account_name("영업이익"), "영업이익")
        self.assertEqual(map_account_name("영업이익(손실)"), "영업이익")

    def test_maps_known_variants_to_canonical_net_income(self):
        self.assertEqual(map_account_name("당기순이익"), "당기순이익")
        self.assertEqual(map_account_name("당기순이익(손실)"), "당기순이익")

    def test_maps_balance_sheet_totals(self):
        self.assertEqual(map_account_name("자산총계"), "자산총계")
        self.assertEqual(map_account_name("부채총계"), "부채총계")
        self.assertEqual(map_account_name("자본총계"), "자본총계")

    def test_unknown_account_returns_none(self):
        self.assertIsNone(map_account_name("기타포괄손익"))

    def test_none_and_empty_input(self):
        self.assertIsNone(map_account_name(None))
        self.assertIsNone(map_account_name(""))

    def test_canonical_accounts_has_six_entries(self):
        self.assertEqual(len(CANONICAL_ACCOUNTS), 6)
