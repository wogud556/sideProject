import shutil
import tempfile
import uuid
from pathlib import Path
from unittest.mock import Mock, patch

from django.test import Client, SimpleTestCase, override_settings

from apps.dart import repository as dart_repository
from apps.dart.normalization import normalize_company_name
from apps.resumes import repository as resume_repository

from .helpers import load_fixture_json, signup_and_login

PATCH_TARGET = "apps.dart.clients.open_dart_client.requests.get"


def _json_response(data: dict) -> Mock:
    response = Mock()
    response.json.return_value = data
    response.content = b""
    response.headers = {}
    return response


class DartApiTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir, DART_API_KEY="test-key")
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        self.user = signup_and_login(self.client)


class CorporationSearchApiTests(DartApiTestBase):
    def test_search_requires_login(self):
        client = Client()
        response = client.get("/api/v1/dart/corporations/search", {"keyword": "삼성"})
        self.assertEqual(response.status_code, 401)

    def test_search_returns_matches(self):
        dart_repository.write_corporations(
            [{"corp_code": "00126380", "corp_name": "삼성전자주식회사", "stock_code": "005930",
              "normalized_name": normalize_company_name("삼성전자주식회사")}]
        )
        response = self.client.get("/api/v1/dart/corporations/search", {"keyword": "삼성전자"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()["results"]), 1)

    def test_search_without_keyword_returns_empty(self):
        response = self.client.get("/api/v1/dart/corporations/search")
        self.assertEqual(response.json(), {"results": []})


class CorporationDetailApiTests(DartApiTestBase):
    def test_invalid_corp_code_returns_400(self):
        response = self.client.get("/api/v1/dart/corporations/abc")
        self.assertEqual(response.status_code, 400)

    def test_success_returns_profile(self):
        fixture = load_fixture_json("dart_company_response.json")
        with patch(PATCH_TARGET, return_value=_json_response(fixture)):
            response = self.client.get("/api/v1/dart/corporations/00126380")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["corp_name"], "삼성전자주식회사")

    def test_missing_api_key_returns_503_not_500(self):
        with override_settings(DART_API_KEY=""):
            response = self.client.get("/api/v1/dart/corporations/00126380")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["error_code"], "DART_API_KEY_MISSING")

    def test_not_found_status_returns_404(self):
        data = {"status": "013", "message": "조회된 데이타가 없습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            response = self.client.get("/api/v1/dart/corporations/00000000")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["error_code"], "DART_NOT_FOUND")


class CorporationFinancialsApiTests(DartApiTestBase):
    def test_cfs_success(self):
        fixture = load_fixture_json("dart_financials_response.json")
        with patch(PATCH_TARGET, return_value=_json_response(fixture)):
            response = self.client.get("/api/v1/dart/corporations/00126380/financials", {"year": "2022"})
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["fs_div"], "CFS")
        self.assertEqual(len(body["accounts"]), 6)

    def test_falls_back_to_ofs_when_cfs_empty(self):
        empty_cfs = {"status": "013", "message": "조회된 데이타가 없습니다."}
        ofs_fixture = load_fixture_json("dart_financials_response.json")
        ofs_fixture = {**ofs_fixture, "list": [{**row, "fs_div": "OFS"} for row in ofs_fixture["list"]]}

        with patch(PATCH_TARGET, side_effect=[_json_response(empty_cfs), _json_response(ofs_fixture)]):
            response = self.client.get("/api/v1/dart/corporations/00126380/financials", {"year": "2022"})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["fs_div"], "OFS")

    def test_rate_limited_returns_429(self):
        data = {"status": "020", "message": "사용한도를 초과하였습니다."}
        with patch(PATCH_TARGET, return_value=_json_response(data)):
            response = self.client.get("/api/v1/dart/corporations/00126380/financials", {"year": "2022"})
        self.assertEqual(response.status_code, 429)

    def test_timeout_returns_504(self):
        import requests

        with patch(PATCH_TARGET, side_effect=requests.exceptions.Timeout("timed out")):
            response = self.client.get("/api/v1/dart/corporations/00126380/financials", {"year": "2022"})
        self.assertEqual(response.status_code, 504)
        self.assertEqual(response.json()["error_code"], "DART_TIMEOUT")

    def test_invalid_year_returns_400_without_calling_dart(self):
        """MED-6 회귀 테스트: 잘못된 year는 DART 호출/캐시 파일 조합 전에 400으로 거부된다."""
        with patch(PATCH_TARGET) as mock_get:
            response = self.client.get(
                "/api/v1/dart/corporations/00126380/financials", {"year": "not-a-year"}
            )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["error_code"], "DART_INVALID_QUERY")
        mock_get.assert_not_called()

    def test_invalid_report_code_returns_400_without_calling_dart(self):
        with patch(PATCH_TARGET) as mock_get:
            response = self.client.get(
                "/api/v1/dart/corporations/00126380/financials",
                {"year": "2022", "report_code": "99999"},
            )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["error_code"], "DART_INVALID_QUERY")
        mock_get.assert_not_called()

    def test_path_traversal_year_returns_400(self):
        with patch(PATCH_TARGET) as mock_get:
            response = self.client.get(
                "/api/v1/dart/corporations/00126380/financials", {"year": "../../../etc"}
            )
        self.assertEqual(response.status_code, 400)
        mock_get.assert_not_called()


class CorporationRefreshApiTests(DartApiTestBase):
    def test_refresh_calls_client_even_if_cached(self):
        fixture = load_fixture_json("dart_company_response.json")
        with patch(PATCH_TARGET, return_value=_json_response(fixture)) as mock_get:
            self.client.get("/api/v1/dart/corporations/00126380")
            self.assertEqual(mock_get.call_count, 1)
            response = self.client.post("/api/v1/dart/corporations/00126380/refresh")
            self.assertEqual(mock_get.call_count, 2)
        self.assertEqual(response.status_code, 200)


class CareerDartCompanyApiTests(DartApiTestBase):
    def setUp(self):
        super().setUp()
        self.resume_id = str(uuid.uuid4())
        resume_repository.write_document(
            self.resume_id,
            {
                "id": self.resume_id,
                "user_id": self.user["id"],
                "original_filename": "resume.pdf",
                "file_size": 1,
                "file_hash": "hash",
                "page_count": 1,
                "is_encrypted": False,
                "analysis_status": "COMPLETED",
                "ocr_used": False,
                "uploaded_at": "2026-01-01T00:00:00+00:00",
                "analysis_completed_at": "2026-01-01T00:00:00+00:00",
                "error_code": None,
                "error_message": None,
                "warnings": [],
            },
        )
        resume_repository.write_profile(
            self.resume_id,
            {
                "basic": {},
                "careers": [
                    {
                        "company_name_raw": "삼성전자주식회사",
                        "company_name_normalized": "삼성전자주식회사",
                        "sort_order": 0,
                        "confidence": 0.9,
                        "review_required": False,
                        "dart_corp_code": None,
                        "dart_match_status": "NOT_SEARCHED",
                    }
                ],
                "educations": [],
                "projects": [],
                "certificates": [],
                "skills": [],
                "languages": [],
                "is_user_confirmed": False,
                "confirmed_at": None,
            },
        )

    def test_link_career_to_company(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data={"corp_code": "00126380"},
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["dart_corp_code"], "00126380")
        self.assertEqual(body["dart_match_status"], "MATCHED_MANUAL")

    def test_invalid_corp_code_returns_400(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data={"corp_code": "abc"},
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)

    def test_null_corp_code_unlinks(self):
        self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data={"corp_code": "00126380"},
            content_type="application/json",
        )
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data={"corp_code": None},
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])
        self.assertEqual(profile["careers"][0]["dart_match_status"], "NOT_SEARCHED")

    def test_non_owner_gets_404(self):
        other_client = Client()
        signup_and_login(other_client, username="other")
        response = other_client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data={"corp_code": "00126380"},
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 404)
