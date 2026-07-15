"""HIGH-1 회귀 테스트: /api/v1/... 상태 변경 엔드포인트는 CSRF 보호를
받아야 한다.

Django의 기본 테스트 Client는 CSRF 검사를 비활성화하므로
(enforce_csrf_checks=False가 기본값), 여기서는 명시적으로
Client(enforce_csrf_checks=True)를 사용해 실제로 CSRF가 강제되는지
확인한다.
"""
import json
import shutil
import tempfile
from pathlib import Path

from django.test import Client, SimpleTestCase, override_settings

from apps.resumes.services import upload_service

from .helpers import build_text_resume_pdf, signup_and_login


class ApiCsrfEnforcedTests(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        # 로그인 자체는 일반 Client(enforce_csrf_checks=False)로 수행한다
        # (helpers.signup_and_login이 폼 필드만 보내므로). 로그인 이후에만
        # CSRF 검사를 켜서, 실제 브라우저가 세션을 얻은 뒤 API를 호출하는
        # 상황과 동일하게 만든다.
        self.client = Client()
        self.user = signup_and_login(self.client)
        self.client.handler.enforce_csrf_checks = True

        self.resume_id = upload_service.upload_resume(self.user["id"], _pdf_upload())

    def test_csrf_token_endpoint_sets_cookie_and_returns_token(self):
        response = self.client.get("/api/v1/csrf")
        self.assertEqual(response.status_code, 200)
        self.assertIn("csrfToken", response.json())
        self.assertIn("csrftoken", response.cookies)

    def test_post_without_csrf_token_is_rejected(self):
        response = self.client.patch(
            f"/api/v1/resumes/{self.resume_id}/profile",
            data=json.dumps({"basic": {"name": "새이름"}}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)

    def test_post_with_valid_csrf_token_header_succeeds(self):
        csrf_response = self.client.get("/api/v1/csrf")
        token = csrf_response.json()["csrfToken"]

        response = self.client.patch(
            f"/api/v1/resumes/{self.resume_id}/profile",
            data=json.dumps({"basic": {"name": "새이름"}}),
            content_type="application/json",
            HTTP_X_CSRFTOKEN=token,
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["basic"]["name"]["value"], "새이름")

    def test_career_create_without_csrf_token_is_rejected(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers",
            data=json.dumps({"company_name_raw": "새 회사"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)

    def test_dart_career_link_without_csrf_token_is_rejected(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers/0/dart-company",
            data=json.dumps({"corp_code": "00126380"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)


def _pdf_upload():
    from django.core.files.uploadedfile import SimpleUploadedFile

    return SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
