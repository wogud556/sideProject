"""SPEC.md 10장(통합 테스트: 업로드→분석→profile.json 생성→수정→확인)과
6장(DART 검색→연결→개황→재무)을 하나의 흐름으로 이어 붙인 end-to-end
시나리오 테스트.

지금까지의 통합 테스트는 이력서 흐름과 DART 흐름을 각각 별도 파일에서
검증했지만, 실제 사용자 여정(업로드 → 분석 → 상세 수정 → 확인 완료 →
기업 매칭 → 기업 상세/재무 조회)을 처음부터 끝까지 한 번에 통과시키는
테스트는 없었다. 이 파일이 그 공백을 메운다. 실제 OpenDART 호출은
전부 mock 처리한다.
"""
import json
import shutil
import tempfile
from pathlib import Path
from unittest.mock import Mock, patch

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client, SimpleTestCase, override_settings

from apps.dart import repository as dart_repository
from apps.dart.normalization import normalize_company_name
from apps.resumes import repository as resume_repository

from .helpers import build_text_resume_pdf, load_fixture_json, signup_and_login

PATCH_TARGET = "apps.dart.clients.open_dart_client.requests.get"


def _json_response(data: dict) -> Mock:
    response = Mock()
    response.json.return_value = data
    response.content = b""
    response.headers = {}
    return response


@override_settings(DART_API_KEY="test-key")
class FullResumeToDartJourneyTests(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        self.user = signup_and_login(self.client)

    def test_upload_to_confirm_to_dart_financials_end_to_end(self):
        # 1) 업로드 → 동기 분석까지 API로 수행된다.
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        upload_response = self.client.post("/api/v1/resumes", {"file": upload})
        self.assertEqual(upload_response.status_code, 201)
        resume_id = upload_response.json()["id"]
        self.assertEqual(upload_response.json()["analysis_status"], "COMPLETED")

        # 2) profile.json이 생성되고 경력 2건(ABC주식회사/한아티)이 추출됐다.
        detail_response = self.client.get(f"/api/v1/resumes/{resume_id}")
        self.assertEqual(detail_response.status_code, 200)
        profile = detail_response.json()["profile"]
        self.assertEqual(profile["basic"]["name"]["value"], "홍길동")
        self.assertEqual(len(profile["careers"]), 2)
        self.assertIn("ABC주식회사", profile["careers"][0]["company_name_raw"])

        # 3) 상세 화면에서 기본정보를 수정한다.
        patch_response = self.client.patch(
            f"/api/v1/resumes/{resume_id}/profile",
            data=json.dumps({"basic": {"name": "홍길동(수정)"}}),
            content_type="application/json",
        )
        self.assertEqual(patch_response.status_code, 200)
        self.assertEqual(patch_response.json()["basic"]["name"]["value"], "홍길동(수정)")

        # 4) 확인 완료 처리.
        confirm_response = self.client.post(f"/api/v1/resumes/{resume_id}/confirm")
        self.assertEqual(confirm_response.status_code, 200)
        self.assertTrue(confirm_response.json()["is_user_confirmed"])

        # 5) DART 후보 검색(mock) — 첫 번째 경력의 회사명으로 검색한다.
        dart_repository.write_corporations(
            [
                {
                    "corp_code": "00126380",
                    "corp_name": "ABC주식회사",
                    "stock_code": "005930",
                    "normalized_name": normalize_company_name("ABC주식회사"),
                }
            ]
        )
        search_response = self.client.get("/api/v1/dart/corporations/search", {"keyword": "ABC주식회사"})
        self.assertEqual(search_response.status_code, 200)
        candidates = search_response.json()["results"]
        self.assertEqual(len(candidates), 1)
        self.assertEqual(candidates[0]["corp_code"], "00126380")

        # 6) 기업 선택 → 경력에 연결.
        link_response = self.client.post(
            f"/api/v1/resumes/{resume_id}/careers/0/dart-company",
            data=json.dumps({"corp_code": "00126380"}),
            content_type="application/json",
        )
        self.assertEqual(link_response.status_code, 200)
        self.assertEqual(link_response.json()["dart_corp_code"], "00126380")
        self.assertEqual(link_response.json()["dart_match_status"], "MATCHED_MANUAL")

        # 7) 기업 개황 조회(mock) — 최초 조회 시 캐시 파일에 저장된다.
        company_fixture = load_fixture_json("dart_company_response.json")
        with patch(PATCH_TARGET, return_value=_json_response(company_fixture)):
            profile_response = self.client.get("/api/v1/dart/corporations/00126380")
        self.assertEqual(profile_response.status_code, 200)
        self.assertEqual(profile_response.json()["corp_name"], "삼성전자주식회사")
        self.assertTrue((self.data_dir / "dart" / "profiles" / "00126380.json").exists())

        # 8) 주요 재무 6계정 조회(mock, CFS).
        financials_fixture = load_fixture_json("dart_financials_response.json")
        with patch(PATCH_TARGET, return_value=_json_response(financials_fixture)):
            financials_response = self.client.get(
                "/api/v1/dart/corporations/00126380/financials", {"year": "2022"}
            )
        self.assertEqual(financials_response.status_code, 200)
        financials_body = financials_response.json()
        self.assertEqual(financials_body["fs_div"], "CFS")
        self.assertEqual(len(financials_body["accounts"]), 6)

        # 9) 최종적으로 이력서 profile.json에 확인·연결 상태가 모두 남아 있다
        # (사용자 수정값도 유지된다).
        final_profile = resume_repository.read_profile(resume_id)
        self.assertTrue(final_profile["is_user_confirmed"])
        self.assertEqual(final_profile["basic"]["name"]["value"], "홍길동(수정)")
        self.assertEqual(final_profile["careers"][0]["dart_corp_code"], "00126380")
        self.assertEqual(final_profile["careers"][0]["dart_match_status"], "MATCHED_MANUAL")
