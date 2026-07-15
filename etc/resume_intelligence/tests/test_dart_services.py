import shutil
import tempfile
from pathlib import Path

from django.test import SimpleTestCase, override_settings

from apps.dart import exceptions, repository
from apps.dart.clients.dto import CompanyProfileDTO, FinancialAccountDTO, FinancialStatementDTO
from apps.dart.services import company_service, sync_service
from apps.dart.normalization import normalize_company_name


class _FakeClient:
    """실제 네트워크 호출 없이 서비스 계층 로직만 검증하기 위한 더블."""

    def __init__(self):
        self.download_corp_codes_calls = 0
        self.get_company_calls = 0
        self.financial_calls = []
        self.raise_on_cfs = False
        self.raise_on_ofs = False
        self.corp_codes_result = [
            {"corp_code": "00126380", "corp_name": "삼성전자주식회사", "corp_eng_name": "", "stock_code": "005930", "modify_date": "20240101"},
            {"corp_code": "00164779", "corp_name": "한아티주식회사", "corp_eng_name": "", "stock_code": "", "modify_date": "20240102"},
        ]

    def is_configured(self):
        return True

    def download_corp_codes(self):
        self.download_corp_codes_calls += 1
        return self.corp_codes_result

    def get_company(self, corp_code):
        self.get_company_calls += 1
        return CompanyProfileDTO(
            corp_code=corp_code,
            corp_name="삼성전자주식회사",
            corp_name_eng="SAMSUNG",
            stock_code="005930",
            ceo_name="한종희",
            corp_cls="Y",
            address="경기도 수원시",
            homepage_url="www.sec.co.kr",
            est_date="19690113",
            raw={"status": "000"},
        )

    def get_financial_accounts(self, corp_code, business_year, report_code, fs_div):
        self.financial_calls.append(fs_div)
        if fs_div == "CFS" and self.raise_on_cfs:
            raise exceptions.DartNotFound("CFS 데이터 없음")
        if fs_div == "OFS" and self.raise_on_ofs:
            raise exceptions.DartNotFound("OFS 데이터 없음")

        accounts = [
            FinancialAccountDTO(canonical_name="매출액", raw_account_name="수익(매출액)", amount="100", fs_div=fs_div),
        ]
        return FinancialStatementDTO(
            corp_code=corp_code,
            business_year=business_year,
            report_code=report_code,
            fs_div=fs_div,
            accounts=accounts,
            raw=[],
        )


class DartServiceTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)


class SyncServiceTests(DartServiceTestBase):
    def test_sync_corporations_writes_normalized_names(self):
        client = _FakeClient()
        count = sync_service.sync_corporations(client=client)

        self.assertEqual(count, 2)
        stored = repository.read_corporations()
        self.assertEqual(len(stored), 2)
        samsung = next(c for c in stored if c["corp_code"] == "00126380")
        self.assertEqual(samsung["normalized_name"], normalize_company_name("삼성전자주식회사"))

    def test_sync_corporations_propagates_dart_errors(self):
        class _FailingClient(_FakeClient):
            def download_corp_codes(self):
                raise exceptions.DartApiKeyMissing("키 없음")

        with self.assertRaises(exceptions.DartApiKeyMissing):
            sync_service.sync_corporations(client=_FailingClient())


class CompanyServiceCacheTests(DartServiceTestBase):
    def test_get_company_profile_calls_client_on_first_request(self):
        client = _FakeClient()
        profile = company_service.get_company_profile("00126380", client=client)

        self.assertEqual(profile["corp_name"], "삼성전자주식회사")
        self.assertEqual(client.get_company_calls, 1)

    def test_get_company_profile_uses_cache_on_second_request(self):
        client = _FakeClient()
        company_service.get_company_profile("00126380", client=client)
        company_service.get_company_profile("00126380", client=client)

        self.assertEqual(client.get_company_calls, 1)

    def test_force_refresh_bypasses_cache(self):
        client = _FakeClient()
        company_service.get_company_profile("00126380", client=client)
        company_service.get_company_profile("00126380", client=client, force_refresh=True)

        self.assertEqual(client.get_company_calls, 2)

    def test_refresh_company_profile_always_calls_client(self):
        client = _FakeClient()
        company_service.get_company_profile("00126380", client=client)
        company_service.refresh_company_profile("00126380", client=client)

        self.assertEqual(client.get_company_calls, 2)


class CompanyServiceFinancialsTests(DartServiceTestBase):
    def test_uses_cfs_when_available(self):
        client = _FakeClient()
        data = company_service.get_financial_accounts("00126380", "2022", client=client)

        self.assertEqual(data["fs_div"], "CFS")
        self.assertEqual(client.financial_calls, ["CFS"])

    def test_falls_back_to_ofs_when_cfs_has_no_accounts(self):
        client = _FakeClient()
        client.raise_on_cfs = True
        data = company_service.get_financial_accounts("00126380", "2022", client=client)

        self.assertEqual(data["fs_div"], "OFS")
        self.assertEqual(client.financial_calls, ["CFS", "OFS"])

    def test_caches_result_after_first_fetch(self):
        client = _FakeClient()
        company_service.get_financial_accounts("00126380", "2022", client=client)
        company_service.get_financial_accounts("00126380", "2022", client=client)

        self.assertEqual(client.financial_calls, ["CFS"])


class FinancialsQueryValidationTests(DartServiceTestBase):
    """MED-6 회귀 테스트: year/report_code가 화이트리스트를 벗어나면 캐시
    파일명을 조합하기 전에 거부되어야 한다."""

    def test_validate_financials_query_accepts_known_values(self):
        company_service.validate_financials_query("2022", "11011")  # 예외 없어야 함

    def test_invalid_year_format_raises_before_touching_cache(self):
        client = _FakeClient()
        with self.assertRaises(exceptions.DartInvalidQuery):
            company_service.get_financial_accounts("00126380", "22", client=client)
        self.assertEqual(client.financial_calls, [])

    def test_non_numeric_year_raises(self):
        with self.assertRaises(exceptions.DartInvalidQuery):
            company_service.validate_financials_query("abcd", "11011")

    def test_path_traversal_year_raises(self):
        with self.assertRaises(exceptions.DartInvalidQuery):
            company_service.validate_financials_query("../../etc", "11011")

    def test_unknown_report_code_raises_before_touching_cache(self):
        client = _FakeClient()
        with self.assertRaises(exceptions.DartInvalidQuery):
            company_service.get_financial_accounts("00126380", "2022", "99999", client=client)
        self.assertEqual(client.financial_calls, [])

    def test_all_whitelisted_report_codes_accepted(self):
        from apps.dart import constants

        for report_code in constants.VALID_REPORT_CODES:
            company_service.validate_financials_query("2022", report_code)


class CompanyServiceSearchTests(DartServiceTestBase):
    def test_search_corporations_matches_normalized_substring(self):
        repository.write_corporations(
            [
                {"corp_code": "00126380", "corp_name": "삼성전자주식회사", "stock_code": "005930", "normalized_name": normalize_company_name("삼성전자주식회사")},
                {"corp_code": "00164779", "corp_name": "한아티주식회사", "stock_code": "", "normalized_name": normalize_company_name("한아티주식회사")},
            ]
        )
        results = company_service.search_corporations("삼성전자")
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["corp_code"], "00126380")

    def test_search_corporations_empty_keyword_returns_empty(self):
        self.assertEqual(company_service.search_corporations(""), [])
