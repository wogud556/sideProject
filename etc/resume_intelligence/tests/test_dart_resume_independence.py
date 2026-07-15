"""DART 미설정/미동기화가 이력서 기능에 영향을 주지 않는지 확인 (SPEC.md 6장).

apps.resumes는 apps.dart를 import하지 않는다(PLAN.md — 의존 방향은
dart → resumes 단방향). 이 테스트는 그 정적 조건과, DART_API_KEY가 없어도
업로드→분석→상세조회→확인 완료 흐름이 정상 동작함을 함께 확인한다.
"""
import ast
import shutil
import tempfile
from pathlib import Path

from django.conf import settings
from django.test import Client, SimpleTestCase, override_settings

from apps.resumes import repository as resume_repository

from .helpers import build_text_resume_pdf, signup_and_login


class ResumesAppDoesNotImportDartTests(SimpleTestCase):
    def test_no_resumes_module_imports_apps_dart(self):
        resumes_dir = Path(settings.BASE_DIR) / "apps" / "resumes"
        offending = []
        for path in resumes_dir.rglob("*.py"):
            tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    names = [alias.name for alias in node.names]
                elif isinstance(node, ast.ImportFrom):
                    names = [node.module] if node.module else []
                else:
                    continue
                if any(name and (name == "apps.dart" or name.startswith("apps.dart.")) for name in names):
                    offending.append(str(path))
        self.assertEqual(offending, [], f"apps.resumes가 apps.dart를 import함: {offending}")


@override_settings(DART_API_KEY="")
class ResumeFlowUnaffectedByMissingDartKeyTests(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        self.user = signup_and_login(self.client)

    def test_upload_analyze_view_confirm_flow_works_without_dart_key(self):
        from django.core.files.uploadedfile import SimpleUploadedFile

        response = self.client.post(
            "/resumes/upload/",
            {"file": SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")},
        )
        self.assertEqual(response.status_code, 302)
        resume_id = response.url.rstrip("/").split("/")[-1]

        detail_response = self.client.get(f"/resumes/{resume_id}/")
        self.assertEqual(detail_response.status_code, 200)
        self.assertContains(detail_response, "홍길동")

        confirm_response = self.client.post(f"/resumes/{resume_id}/confirm/")
        self.assertEqual(confirm_response.status_code, 302)

        profile = resume_repository.read_profile(resume_id)
        self.assertTrue(profile["is_user_confirmed"])
