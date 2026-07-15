import shutil
import tempfile
from pathlib import Path
from unittest.mock import patch

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client, SimpleTestCase, override_settings

from apps.pdf_analysis.schemas.document import AnalysisStatus
from apps.pdf_analysis.validators.file_validators import PdfValidationError
from apps.resumes import repository
from apps.resumes.services import upload_service

from .helpers import (
    build_encrypted_pdf,
    build_image_only_pdf,
    build_non_pdf_bytes,
    build_text_resume_pdf,
    signup_and_login,
)


class ResumeTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)


class UploadServiceTests(ResumeTestBase):
    def test_upload_resume_runs_full_pipeline_synchronously(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        resume_id = upload_service.upload_resume("user-1", upload)

        document = repository.read_document(resume_id)
        self.assertEqual(document["analysis_status"], AnalysisStatus.COMPLETED.value)
        self.assertEqual(document["user_id"], "user-1")

        profile = repository.read_profile(resume_id)
        self.assertEqual(profile["basic"]["name"]["value"], "홍길동")

        blocks = repository.read_blocks(resume_id)
        self.assertGreater(len(blocks), 0)

        self.assertTrue(repository.original_pdf_path(resume_id).exists())

    def test_upload_resume_rejects_invalid_pdf_without_creating_record(self):
        upload = SimpleUploadedFile("resume.pdf", build_non_pdf_bytes(), content_type="application/pdf")
        with self.assertRaises(PdfValidationError):
            upload_service.upload_resume("user-1", upload)
        self.assertEqual(repository.list_all_resume_ids(), [])

    def test_upload_resume_rejects_encrypted_pdf(self):
        upload = SimpleUploadedFile("resume.pdf", build_encrypted_pdf(), content_type="application/pdf")
        with self.assertRaises(PdfValidationError) as ctx:
            upload_service.upload_resume("user-1", upload)
        self.assertEqual(ctx.exception.code, "ENCRYPTED_PDF")

    def test_duplicate_upload_adds_warning(self):
        content = build_text_resume_pdf()
        upload1 = SimpleUploadedFile("resume.pdf", content, content_type="application/pdf")
        upload_service.upload_resume("user-1", upload1)

        upload2 = SimpleUploadedFile("resume2.pdf", content, content_type="application/pdf")
        resume_id2 = upload_service.upload_resume("user-1", upload2)

        document2 = repository.read_document(resume_id2)
        self.assertIn("DUPLICATE_FILE_HASH_DETECTED", document2["warnings"])
        self.assertEqual(document2["analysis_status"], AnalysisStatus.COMPLETED_WITH_WARNINGS.value)

    def test_scanned_pdf_triggers_ocr_and_completes(self):
        upload = SimpleUploadedFile(
            "scanned.pdf",
            build_image_only_pdf(["HONG GILDONG", "email: hong@example.com"]),
            content_type="application/pdf",
        )
        resume_id = upload_service.upload_resume("user-1", upload)
        document = repository.read_document(resume_id)
        self.assertTrue(document["ocr_used"])

    def test_special_character_filename_is_preserved(self):
        """경계: 한글/공백/괄호/특수문자가 섞인 정상적인 파일명은 그대로 저장돼야 한다."""
        upload = SimpleUploadedFile(
            "이력서(최종)_홍길동 #1&2.pdf", build_text_resume_pdf(), content_type="application/pdf"
        )
        resume_id = upload_service.upload_resume("user-1", upload)
        document = repository.read_document(resume_id)
        self.assertEqual(document["original_filename"], "이력서(최종)_홍길동 #1&2.pdf")

    def test_lone_surrogate_filename_does_not_crash_upload(self):
        """경계/버그 회귀: 깨진 멀티파트 헤더 등으로 파일명에 lone surrogate가
        섞여 들어와도 업로드가 500으로 죽지 않고, JSON에 안전한 값으로 저장돼야
        한다(수정 전에는 json.dump(ensure_ascii=False)가 UnicodeEncodeError로
        실패했다)."""
        upload = SimpleUploadedFile("resume\udcff.pdf", build_text_resume_pdf(), content_type="application/pdf")
        resume_id = upload_service.upload_resume("user-1", upload)
        document = repository.read_document(resume_id)
        self.assertEqual(document["original_filename"], "resume?.pdf")
        self.assertEqual(document["analysis_status"], AnalysisStatus.COMPLETED.value)

    def test_pipeline_exception_results_in_failed_status(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        with patch("apps.resumes.services.upload_service.run_pipeline", side_effect=RuntimeError("boom")):
            resume_id = upload_service.upload_resume("user-1", upload)

        document = repository.read_document(resume_id)
        self.assertEqual(document["analysis_status"], AnalysisStatus.FAILED.value)
        self.assertEqual(document["error_code"], "PIPELINE_ERROR")
        # 원문 예외 메시지를 그대로 노출하지 않는다(개인정보/내부 정보 보호).
        self.assertNotIn("boom", document["error_message"])


class UploadViewTests(ResumeTestBase):
    def setUp(self):
        super().setUp()
        self.client = Client()
        self.user = signup_and_login(self.client)

    def test_requires_login(self):
        anon_client = Client()
        response = anon_client.get("/resumes/upload/")
        self.assertEqual(response.status_code, 302)

    def test_get_renders_form(self):
        response = self.client.get("/resumes/upload/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "이력서 업로드")

    def test_post_valid_pdf_redirects_to_detail(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        response = self.client.post("/resumes/upload/", {"file": upload})
        self.assertEqual(response.status_code, 302)
        self.assertIn("/resumes/", response.url)

    def test_post_invalid_pdf_shows_form_error(self):
        upload = SimpleUploadedFile("resume.txt", build_non_pdf_bytes(), content_type="text/plain")
        response = self.client.post("/resumes/upload/", {"file": upload})
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "PDF 파일만 업로드할 수 있습니다")

    def test_uploaded_resume_appears_on_dashboard(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.client.post("/resumes/upload/", {"file": upload})

        response = self.client.get("/resumes/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "resume.pdf")
