"""소유자 검사 / 경로 조작 방지 (SPEC.md 8장, PLAN.md Phase 7).

비소유자 접근은 404, UUID가 아닌 문자열은 URL 컨버터 단계에서 404,
corp_code가 8자리 숫자가 아니면 400/404로 차단되는지 확인한다.
"""
import shutil
import tempfile
from pathlib import Path

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client, SimpleTestCase, override_settings

from apps.resumes.services import upload_service

from .helpers import build_text_resume_pdf, signup_and_login


class PermissionsTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.owner_client = Client()
        self.owner = signup_and_login(self.owner_client)
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.resume_id = upload_service.upload_resume(self.owner["id"], upload)

        self.stranger_client = Client()
        self.stranger = signup_and_login(self.stranger_client, username="stranger")


class NonOwnerTemplateViewTests(PermissionsTestBase):
    def test_detail_view_404_for_non_owner(self):
        response = self.stranger_client.get(f"/resumes/{self.resume_id}/")
        self.assertEqual(response.status_code, 404)

    def test_pdf_view_404_for_non_owner(self):
        response = self.stranger_client.get(f"/resumes/{self.resume_id}/pdf/")
        self.assertEqual(response.status_code, 404)

    def test_excel_view_404_for_non_owner(self):
        response = self.stranger_client.get(f"/resumes/{self.resume_id}/excel/")
        self.assertEqual(response.status_code, 404)

    def test_raw_text_view_404_for_non_owner(self):
        response = self.stranger_client.get(f"/resumes/{self.resume_id}/raw-text/")
        self.assertEqual(response.status_code, 404)

    def test_delete_view_404_for_non_owner_and_resume_untouched(self):
        response = self.stranger_client.post(f"/resumes/{self.resume_id}/delete/")
        self.assertEqual(response.status_code, 404)
        # 소유자는 여전히 접근 가능해야 한다(삭제되지 않음).
        owner_response = self.owner_client.get(f"/resumes/{self.resume_id}/")
        self.assertEqual(owner_response.status_code, 200)

    def test_confirm_view_404_for_non_owner(self):
        response = self.stranger_client.post(f"/resumes/{self.resume_id}/confirm/")
        self.assertEqual(response.status_code, 404)

    def test_career_delete_view_404_for_non_owner(self):
        response = self.stranger_client.post(f"/resumes/{self.resume_id}/careers/0/delete/")
        self.assertEqual(response.status_code, 404)


class NonOwnerApiTests(PermissionsTestBase):
    def test_api_detail_404_for_non_owner(self):
        response = self.stranger_client.get(f"/api/v1/resumes/{self.resume_id}")
        self.assertEqual(response.status_code, 404)

    def test_api_404_response_is_json_not_html(self):
        """LOW-2 회귀 테스트: API에서 발생한 Http404는 Django 기본 HTML
        404 페이지가 아니라 JSON으로 응답해야 한다."""
        response = self.stranger_client.get(f"/api/v1/resumes/{self.resume_id}")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
        body = response.json()
        self.assertEqual(body["error_code"], "NOT_FOUND")

    def test_api_profile_patch_404_for_non_owner(self):
        response = self.stranger_client.patch(
            f"/api/v1/resumes/{self.resume_id}/profile",
            data="{}",
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")

    def test_api_career_delete_404_for_non_owner(self):
        response = self.stranger_client.delete(f"/api/v1/resumes/{self.resume_id}/careers/0")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")

    def test_api_out_of_range_career_index_returns_json_404(self):
        response = self.owner_client.delete(f"/api/v1/resumes/{self.resume_id}/careers/999")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
        self.assertEqual(response.json()["error_code"], "NOT_FOUND")


class NonExistentAndMalformedIdTests(PermissionsTestBase):
    def test_random_uuid_returns_404(self):
        import uuid

        response = self.owner_client.get(f"/resumes/{uuid.uuid4()}/")
        self.assertEqual(response.status_code, 404)

    def test_raw_text_random_uuid_returns_404(self):
        import uuid

        response = self.owner_client.get(f"/resumes/{uuid.uuid4()}/raw-text/")
        self.assertEqual(response.status_code, 404)

    def test_non_uuid_path_segment_returns_404_via_url_converter(self):
        response = self.owner_client.get("/resumes/../../etc/passwd/")
        self.assertEqual(response.status_code, 404)

    def test_sql_like_injection_string_returns_404(self):
        response = self.owner_client.get("/resumes/1%20OR%201=1/")
        self.assertEqual(response.status_code, 404)


class CorpCodeValidationTests(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir, DART_API_KEY="test-key")
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        signup_and_login(self.client)

    def test_non_numeric_corp_code_rejected(self):
        response = self.client.get("/api/v1/dart/corporations/not-a-code")
        self.assertEqual(response.status_code, 400)

    def test_path_traversal_corp_code_rejected(self):
        response = self.client.get("/api/v1/dart/corporations/../../etc")
        self.assertIn(response.status_code, (400, 404))

    def test_too_short_corp_code_rejected(self):
        response = self.client.get("/api/v1/dart/corporations/123")
        self.assertEqual(response.status_code, 400)


class CorpCodeRepositoryDefenseInDepthTests(SimpleTestCase):
    """MED-4 회귀 테스트: repository의 경로 조합 함수 자체가 corp_code를
    재검증하는지 확인한다(호출부 검증을 우회해도 경로 조작이 불가능해야 함)."""

    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

    def test_profile_path_rejects_invalid_corp_code(self):
        from apps.dart import repository as dart_repository

        with self.assertRaises(ValueError):
            dart_repository.profile_path("../../etc/passwd")

    def test_financials_path_rejects_invalid_corp_code(self):
        from apps.dart import repository as dart_repository

        with self.assertRaises(ValueError):
            dart_repository.financials_path("not-8-digits", "2022", "11011")

    def test_profile_path_accepts_valid_corp_code(self):
        from apps.dart import repository as dart_repository

        path = dart_repository.profile_path("00126380")
        self.assertIn("00126380.json", str(path))
