from unittest.mock import Mock, patch

import requests
from django.test import SimpleTestCase, override_settings

from apps.dart import exceptions
from apps.dart.clients.open_dart_client import OpenDartClient

from .helpers import build_corp_code_zip_bytes, load_fixture_json

PATCH_TARGET = "apps.dart.clients.open_dart_client.requests.get"


def _json_response(data: dict, status_code: int = 200) -> Mock:
    response = Mock()
    response.status_code = status_code
    response.json.return_value = data
    response.content = b""
    response.headers = {}
    return response


class IsConfiguredTests(SimpleTestCase):
    def test_configured_when_key_provided(self):
        self.assertTrue(OpenDartClient(api_key="test-key").is_configured())

    def test_not_configured_when_key_empty(self):
        self.assertFalse(OpenDartClient(api_key="").is_configured())

    @override_settings(DART_API_KEY="")
    def test_falls_back_to_settings_when_key_not_passed(self):
        self.assertFalse(OpenDartClient().is_configured())


class GetCompanyTests(SimpleTestCase):
    def setUp(self):
        self.client = OpenDartClient(api_key="test-key")
        self.fixture = load_fixture_json("dart_company_response.json")

    def test_success_returns_dto(self):
        with patch(PATCH_TARGET, return_value=_json_response(self.fixture)):
            dto = self.client.get_company("00126380")
        self.assertEqual(dto.corp_code, "00126380")
        self.assertEqual(dto.corp_name, "삼성전자주식회사")
        self.assertEqual(dto.stock_code, "005930")
        self.assertEqual(dto.raw["status"], "000")

    def test_status_013_raises_not_found(self):
        data = {"status": "013", "message": "조회된 데이타가 없습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartNotFound):
                self.client.get_company("99999999")

    def test_status_010_raises_api_key_missing(self):
        data = {"status": "010", "message": "등록되지 않은 키입니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartApiKeyMissing):
                self.client.get_company("00126380")

    def test_status_020_raises_rate_limited(self):
        data = {"status": "020", "message": "사용한도를 초과하였습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartRateLimited):
                self.client.get_company("00126380")

    def test_status_800_raises_api_error(self):
        data = {"status": "800", "message": "시스템 점검 중입니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartApiError):
                self.client.get_company("00126380")

    def test_status_900_raises_timeout(self):
        data = {"status": "900", "message": "정의되지 않은 오류가 발생하였습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartTimeout):
                self.client.get_company("00126380")

    def test_network_timeout_raises_dart_timeout(self):
        with patch(PATCH_TARGET, side_effect=requests.exceptions.Timeout("timed out")):
            with self.assertRaises(exceptions.DartTimeout):
                self.client.get_company("00126380")

    def test_network_error_raises_api_error(self):
        with patch(PATCH_TARGET, side_effect=requests.exceptions.ConnectionError("boom")):
            with self.assertRaises(exceptions.DartApiError):
                self.client.get_company("00126380")

    def test_missing_key_raises_before_request(self):
        client = OpenDartClient(api_key="")
        with patch(PATCH_TARGET) as mock_get:
            with self.assertRaises(exceptions.DartApiKeyMissing):
                client.get_company("00126380")
            mock_get.assert_not_called()

    def test_non_json_response_raises_api_error_not_json_decode_error(self):
        """MED-5 회귀 테스트: response.json()이 비-JSON 본문에서 던지는
        ValueError(JSONDecodeError)가 그대로 새지 않고 DartApiError로
        변환되어야 한다(그렇지 않으면 화면이 500으로 죽는다)."""
        response = Mock()
        response.status_code = 200
        response.json.side_effect = ValueError("Expecting value: line 1 column 1 (char 0)")
        response.content = b"<html>502 Bad Gateway</html>"
        response.headers = {}
        response.raise_for_status = Mock()

        with patch(PATCH_TARGET, return_value=response):
            with self.assertRaises(exceptions.DartApiError):
                self.client.get_company("00126380")

    def test_http_error_status_raises_api_error(self):
        """response.raise_for_status()가 던지는 HTTPError도 DartApiError로 매핑된다."""
        response = Mock()
        response.status_code = 500
        response.raise_for_status.side_effect = requests.exceptions.HTTPError("500 Server Error")

        with patch(PATCH_TARGET, return_value=response):
            with self.assertRaises(exceptions.DartApiError):
                self.client.get_company("00126380")


class GetFinancialAccountsTests(SimpleTestCase):
    def setUp(self):
        self.client = OpenDartClient(api_key="test-key")
        self.fixture = load_fixture_json("dart_financials_response.json")

    def test_success_maps_key_accounts_and_keeps_raw_names(self):
        with patch(PATCH_TARGET, return_value=_json_response(self.fixture)):
            dto = self.client.get_financial_accounts("00126380", "2022", "11011", "CFS")

        canonical_names = {a.canonical_name for a in dto.accounts}
        self.assertEqual(
            canonical_names, {"자산총계", "부채총계", "자본총계", "매출액", "영업이익", "당기순이익"}
        )
        revenue = next(a for a in dto.accounts if a.canonical_name == "매출액")
        self.assertEqual(revenue.raw_account_name, "수익(매출액)")
        self.assertEqual(revenue.fs_div, "CFS")

    def test_no_data_status_raises_not_found(self):
        data = {"status": "013", "message": "조회된 데이타가 없습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            with self.assertRaises(exceptions.DartNotFound):
                self.client.get_financial_accounts("00126380", "2022", "11011", "OFS")


class DownloadCorpCodesTests(SimpleTestCase):
    def setUp(self):
        self.client = OpenDartClient(api_key="test-key")

    def test_success_parses_zip_xml(self):
        response = Mock()
        response.content = build_corp_code_zip_bytes()
        response.headers = {"Content-Type": "application/x-msdownload"}
        with patch(PATCH_TARGET, return_value=response):
            corporations = self.client.download_corp_codes()

        self.assertEqual(len(corporations), 4)
        codes = {c["corp_code"] for c in corporations}
        self.assertIn("00126380", codes)
        samsung = next(c for c in corporations if c["corp_code"] == "00126380")
        self.assertEqual(samsung["corp_name"], "삼성전자주식회사")
        self.assertEqual(samsung["stock_code"], "005930")

    def test_error_xml_response_raises_mapped_exception(self):
        import xml.etree.ElementTree as ET

        error_xml = ET.tostring(
            ET.fromstring("<result><status>010</status><message>등록되지 않은 키입니다.</message></result>")
        )
        response = Mock()
        response.content = error_xml
        response.headers = {"Content-Type": "text/xml"}
        with patch(PATCH_TARGET, return_value=response):
            with self.assertRaises(exceptions.DartApiKeyMissing):
                self.client.download_corp_codes()

    def test_timeout_raises_dart_timeout(self):
        with patch(PATCH_TARGET, side_effect=requests.exceptions.Timeout("timed out")):
            with self.assertRaises(exceptions.DartTimeout):
                self.client.download_corp_codes()

    def test_missing_key_raises_before_request(self):
        client = OpenDartClient(api_key="")
        with patch(PATCH_TARGET) as mock_get:
            with self.assertRaises(exceptions.DartApiKeyMissing):
                client.download_corp_codes()
            mock_get.assert_not_called()
