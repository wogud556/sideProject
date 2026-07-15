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


class DartViewsTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir, DART_API_KEY="test-key")
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        self.user = signup_and_login(self.client)

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


class CareerSearchViewTests(DartViewsTestBase):
    def test_requires_login(self):
        client = Client()
        response = client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 302)

    def test_not_synced_shows_guidance_without_crashing(self):
        response = self.client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "동기화되지 않았습니다")

    def test_single_exact_match_shown_but_not_linked_on_get(self):
        """GET은 부수효과가 없어야 한다(MED-2) — 후보만 보여주고 profile.json은 쓰지 않는다."""
        dart_repository.write_corporations(
            [
                {
                    "corp_code": "00126380",
                    "corp_name": "삼성전자주식회사",
                    "stock_code": "005930",
                    "normalized_name": normalize_company_name("삼성전자주식회사"),
                }
            ]
        )
        response = self.client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "자동 매칭 후보를 찾았습니다")

        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])
        self.assertEqual(profile["careers"][0]["dart_match_status"], "NOT_SEARCHED")

    def test_synced_but_no_match_shows_not_found_without_linking(self):
        """인수 시나리오: DART에 동기화는 됐지만 해당 기업이 목록에 없는(미등록)
        경우 후보 없음을 안내하고 아무것도 연결하지 않는다("동기화되지
        않았습니다" 안내와는 다른 케이스 — NOT_FOUND)."""
        dart_repository.write_corporations(
            [
                {
                    "corp_code": "00999999",
                    "corp_name": "전혀다른회사",
                    "stock_code": "",
                    "normalized_name": normalize_company_name("전혀다른회사"),
                }
            ]
        )
        response = self.client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "후보 기업이 없습니다")

        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])
        self.assertEqual(profile["careers"][0]["dart_match_status"], "NOT_SEARCHED")

    def test_confirm_auto_action_links_company(self):
        """자동 매칭 확인은 명시적 POST(action=confirm_auto)로만 이뤄진다."""
        dart_repository.write_corporations(
            [
                {
                    "corp_code": "00126380",
                    "corp_name": "삼성전자주식회사",
                    "stock_code": "005930",
                    "normalized_name": normalize_company_name("삼성전자주식회사"),
                }
            ]
        )
        response = self.client.post(
            f"/dart/resumes/{self.resume_id}/careers/0/search/",
            {"action": "confirm_auto", "corp_code": "00126380"},
        )
        self.assertEqual(response.status_code, 302)

        profile = resume_repository.read_profile(self.resume_id)
        self.assertEqual(profile["careers"][0]["dart_corp_code"], "00126380")
        self.assertEqual(profile["careers"][0]["dart_match_status"], "MATCHED_AUTO")

    def test_cross_site_get_does_not_link(self):
        """크로스사이트 GET(예: 이미지 태그로 유도)으로도 연결이 트리거되면 안 된다."""
        dart_repository.write_corporations(
            [
                {
                    "corp_code": "00126380",
                    "corp_name": "삼성전자주식회사",
                    "stock_code": "005930",
                    "normalized_name": normalize_company_name("삼성전자주식회사"),
                }
            ]
        )
        for _ in range(3):
            self.client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")

        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])

    def test_multiple_candidates_requires_manual_selection(self):
        dart_repository.write_corporations(
            [
                {"corp_code": "00401731", "corp_name": "삼성전자서비스", "stock_code": "",
                 "normalized_name": normalize_company_name("삼성전자서비스")},
                {"corp_code": "00500001", "corp_name": "삼성전자로지텍", "stock_code": "",
                 "normalized_name": normalize_company_name("삼성전자로지텍")},
            ]
        )
        response = self.client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 200)

        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])

    def test_manual_select_links_company(self):
        dart_repository.write_corporations(
            [{"corp_code": "00126380", "corp_name": "삼성전자주식회사", "stock_code": "005930",
              "normalized_name": normalize_company_name("삼성전자주식회사")}]
        )
        response = self.client.post(
            f"/dart/resumes/{self.resume_id}/careers/0/search/",
            {"action": "select", "corp_code": "00126380"},
        )
        self.assertEqual(response.status_code, 302)
        profile = resume_repository.read_profile(self.resume_id)
        self.assertEqual(profile["careers"][0]["dart_corp_code"], "00126380")
        self.assertEqual(profile["careers"][0]["dart_match_status"], "MATCHED_MANUAL")

    def test_unlink_clears_link(self):
        dart_repository.write_corporations(
            [{"corp_code": "00126380", "corp_name": "삼성전자주식회사", "stock_code": "005930",
              "normalized_name": normalize_company_name("삼성전자주식회사")}]
        )
        self.client.post(
            f"/dart/resumes/{self.resume_id}/careers/0/search/",
            {"action": "select", "corp_code": "00126380"},
        )
        self.client.post(
            f"/dart/resumes/{self.resume_id}/careers/0/search/",
            {"action": "unlink"},
        )
        profile = resume_repository.read_profile(self.resume_id)
        self.assertIsNone(profile["careers"][0]["dart_corp_code"])
        self.assertEqual(profile["careers"][0]["dart_match_status"], "NOT_SEARCHED")

    def test_non_owner_gets_404(self):
        other_client = Client()
        signup_and_login(other_client, username="other2")
        response = other_client.get(f"/dart/resumes/{self.resume_id}/careers/0/search/")
        self.assertEqual(response.status_code, 404)


class CompanyDetailViewTests(DartViewsTestBase):
    def test_invalid_corp_code_is_404(self):
        response = self.client.get("/dart/corporations/abc/")
        self.assertEqual(response.status_code, 404)

    def test_shows_profile_and_financials_and_source_notice(self):
        company_fixture = load_fixture_json("dart_company_response.json")
        financials_fixture = load_fixture_json("dart_financials_response.json")

        with patch(
            PATCH_TARGET,
            side_effect=[_json_response(company_fixture), _json_response(financials_fixture)],
        ):
            response = self.client.get("/dart/corporations/00126380/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "삼성전자주식회사")
        self.assertContains(response, "금융감독원 전자공시시스템")

    def test_dart_error_shows_message_instead_of_500(self):
        with override_settings(DART_API_KEY=""):
            response = self.client.get("/dart/corporations/00126380/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "조회 실패")
