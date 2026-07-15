import json
import shutil
import tempfile
from pathlib import Path

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client, SimpleTestCase, override_settings

from apps.resumes import repository
from apps.resumes.services import upload_service

from .helpers import build_text_resume_pdf, signup_and_login


class ResumeApiTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        self.client = Client()
        self.user = signup_and_login(self.client)


class ResumeListCreateApiTests(ResumeApiTestBase):
    def test_requires_login(self):
        anon = Client()
        response = anon.get("/api/v1/resumes")
        self.assertEqual(response.status_code, 401)

    def test_post_uploads_and_analyzes(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        response = self.client.post("/api/v1/resumes", {"file": upload})
        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertEqual(body["analysis_status"], "COMPLETED")

    def test_post_invalid_pdf_returns_400(self):
        upload = SimpleUploadedFile("resume.txt", b"not a pdf", content_type="text/plain")
        response = self.client.post("/api/v1/resumes", {"file": upload})
        self.assertEqual(response.status_code, 400)
        self.assertIn("error_code", response.json())

    def test_get_lists_only_own_resumes(self):
        upload_service.upload_resume(self.user["id"], SimpleUploadedFile("a.pdf", build_text_resume_pdf(), content_type="application/pdf"))
        upload_service.upload_resume("someone-else", SimpleUploadedFile("b.pdf", build_text_resume_pdf(), content_type="application/pdf"))

        response = self.client.get("/api/v1/resumes")
        self.assertEqual(response.status_code, 200)
        results = response.json()["results"]
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["original_filename"], "a.pdf")


class ResumeDetailApiTests(ResumeApiTestBase):
    def setUp(self):
        super().setUp()
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_get_detail_returns_document_and_profile(self):
        response = self.client.get(f"/api/v1/resumes/{self.resume_id}")
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["document"]["id"], self.resume_id)
        self.assertEqual(body["profile"]["basic"]["name"]["value"], "홍길동")

    def test_get_status(self):
        response = self.client.get(f"/api/v1/resumes/{self.resume_id}/status")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["analysis_status"], "COMPLETED")

    def test_non_owner_gets_404(self):
        other = Client()
        signup_and_login(other, username="other_api")
        response = other.get(f"/api/v1/resumes/{self.resume_id}")
        self.assertEqual(response.status_code, 404)

    def test_non_uuid_path_returns_404(self):
        response = self.client.get("/api/v1/resumes/not-a-uuid")
        self.assertEqual(response.status_code, 404)

    def test_patch_profile_updates_basic_field(self):
        response = self.client.patch(
            f"/api/v1/resumes/{self.resume_id}/profile",
            data=json.dumps({"basic": {"name": "새이름"}}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["basic"]["name"]["value"], "새이름")

    def test_confirm(self):
        response = self.client.post(f"/api/v1/resumes/{self.resume_id}/confirm")
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()["is_user_confirmed"])

    def test_reanalyze(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/reanalyze",
            data=json.dumps({"overwrite_user_edits": False}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["analysis_status"], "COMPLETED")


class CareerApiTests(ResumeApiTestBase):
    def setUp(self):
        super().setUp()
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_create_career(self):
        response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/careers",
            data=json.dumps({"company_name_raw": "새 회사"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["company_name_raw"], "새 회사")

    def test_patch_career(self):
        response = self.client.patch(
            f"/api/v1/resumes/{self.resume_id}/careers/0",
            data=json.dumps({"position": "리드"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["position"], "리드")

    def test_delete_career(self):
        response = self.client.delete(f"/api/v1/resumes/{self.resume_id}/careers/0")
        self.assertEqual(response.status_code, 204)
        profile = repository.read_profile(self.resume_id)
        self.assertEqual(len(profile["careers"]), 1)

    def test_patch_out_of_range_returns_404(self):
        response = self.client.patch(
            f"/api/v1/resumes/{self.resume_id}/careers/999",
            data=json.dumps({"position": "x"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 404)


class EducationApiTests(ResumeApiTestBase):
    def setUp(self):
        super().setUp()
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_create_and_delete_education(self):
        create_response = self.client.post(
            f"/api/v1/resumes/{self.resume_id}/educations",
            data=json.dumps({"school_name": "새 학교"}),
            content_type="application/json",
        )
        self.assertEqual(create_response.status_code, 201)

        delete_response = self.client.delete(f"/api/v1/resumes/{self.resume_id}/educations/1")
        self.assertEqual(delete_response.status_code, 204)
