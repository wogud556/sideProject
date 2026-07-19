import shutil
import tempfile
from pathlib import Path

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client, SimpleTestCase, override_settings
from django.urls import reverse

from apps.pdf_analysis.schemas.profile import Career, Project, ResumeProfile
from apps.resumes import repository
from apps.resumes.services import careers_service, upload_service

from .helpers import build_text_resume_pdf, signup_and_login


class ResumeEditingTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)

        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.user_id = "user-1"
        self.resume_id = upload_service.upload_resume(self.user_id, upload)


class CareersServiceTests(ResumeEditingTestBase):
    def test_add_career_appends_with_next_sort_order(self):
        career = careers_service.add_career(
            self.resume_id, self.user_id, {"company_name_raw": "새 회사", "start_date": "2023-01"}
        )
        self.assertEqual(career["sort_order"], 2)  # 기존 2건 이후
        profile = repository.read_profile(self.resume_id)
        self.assertEqual(len(profile["careers"]), 3)

    def test_update_career_changes_field(self):
        updated = careers_service.update_career(self.resume_id, self.user_id, 0, {"position": "수석 개발자"})
        self.assertEqual(updated["position"], "수석 개발자")

    def test_delete_career_reindexes_sort_order(self):
        careers_service.delete_career(self.resume_id, self.user_id, 0)
        profile = repository.read_profile(self.resume_id)
        self.assertEqual(len(profile["careers"]), 1)
        self.assertEqual(profile["careers"][0]["sort_order"], 0)

    def test_delete_out_of_range_raises_404(self):
        from django.http import Http404

        with self.assertRaises(Http404):
            careers_service.delete_career(self.resume_id, self.user_id, 99)

    def test_add_education(self):
        education = careers_service.add_education(
            self.resume_id, self.user_id, {"school_name": "새 학교"}
        )
        self.assertEqual(education["school_name"], "새 학교")

    def test_update_education(self):
        updated = careers_service.update_education(self.resume_id, self.user_id, 0, {"major": "전산학"})
        self.assertEqual(updated["major"], "전산학")

    def test_delete_education(self):
        careers_service.delete_education(self.resume_id, self.user_id, 0)
        profile = repository.read_profile(self.resume_id)
        self.assertEqual(len(profile["educations"]), 0)

    def test_other_user_cannot_edit(self):
        from django.http import Http404

        with self.assertRaises(Http404):
            careers_service.add_career(self.resume_id, "other-user", {"company_name_raw": "x"})


class PatchProfileTests(ResumeEditingTestBase):
    def test_patch_basic_field_value_only(self):
        updated = careers_service.patch_profile(self.resume_id, self.user_id, {"basic": {"name": "김철수"}})
        self.assertEqual(updated["basic"]["name"]["value"], "김철수")

    def test_patch_replaces_skills_list(self):
        updated = careers_service.patch_profile(
            self.resume_id, self.user_id, {"skills": [{"name": "Go"}, {"name": "Rust"}]}
        )
        self.assertEqual([s["name"] for s in updated["skills"]], ["Go", "Rust"])

    def test_patch_is_user_confirmed(self):
        updated = careers_service.patch_profile(self.resume_id, self.user_id, {"is_user_confirmed": True})
        self.assertTrue(updated["is_user_confirmed"])


class ConfirmResumeTests(ResumeEditingTestBase):
    def test_confirm_sets_flag_and_timestamp(self):
        profile = careers_service.confirm_resume(self.resume_id, self.user_id)
        self.assertTrue(profile["is_user_confirmed"])
        self.assertIsNotNone(profile["confirmed_at"])


class ReanalyzeServiceTests(ResumeEditingTestBase):
    def test_reanalyze_preserves_user_edits_by_default(self):
        careers_service.patch_profile(self.resume_id, self.user_id, {"basic": {"name": "사용자수정이름"}})

        upload_service.reanalyze_resume(self.resume_id, self.user_id, overwrite_user_edits=False)

        profile = repository.read_profile(self.resume_id)
        self.assertEqual(profile["basic"]["name"]["value"], "사용자수정이름")

    def test_reanalyze_with_overwrite_replaces_user_edits(self):
        careers_service.patch_profile(self.resume_id, self.user_id, {"basic": {"name": "사용자수정이름"}})

        upload_service.reanalyze_resume(self.resume_id, self.user_id, overwrite_user_edits=True)

        profile = repository.read_profile(self.resume_id)
        self.assertEqual(profile["basic"]["name"]["value"], "홍길동")

    def test_reanalyze_preserves_confirmation_state(self):
        careers_service.confirm_resume(self.resume_id, self.user_id)
        upload_service.reanalyze_resume(self.resume_id, self.user_id, overwrite_user_edits=False)

        profile = repository.read_profile(self.resume_id)
        self.assertTrue(profile["is_user_confirmed"])


class DetailEditViewTests(ResumeEditingTestBase):
    def setUp(self):
        super().setUp()
        # 위 setUp은 user_id="user-1" 문자열을 그대로 썼으므로, 뷰 테스트는
        # 실제 로그인 세션이 필요해 별도 사용자로 새로 업로드한다.
        self.client = Client()
        self.user = signup_and_login(self.client)
        upload = SimpleUploadedFile("resume2.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.view_resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_basic_info_edit_view_updates_name(self):
        response = self.client.post(
            f"/resumes/{self.view_resume_id}/basic/", {"name": "새이름", "email": "new@example.com"}
        )
        self.assertEqual(response.status_code, 302)
        profile = repository.read_profile(self.view_resume_id)
        self.assertEqual(profile["basic"]["name"]["value"], "새이름")

    def test_career_add_view(self):
        response = self.client.post(
            f"/resumes/{self.view_resume_id}/careers/add/",
            {"company_name_raw": "새 회사", "start_date": "2024-01", "responsibilities": "업무1\n업무2"},
        )
        self.assertEqual(response.status_code, 302)
        profile = repository.read_profile(self.view_resume_id)
        self.assertEqual(profile["careers"][-1]["company_name_raw"], "새 회사")
        self.assertEqual(profile["careers"][-1]["responsibilities"], ["업무1", "업무2"])

    def test_career_delete_view(self):
        response = self.client.post(f"/resumes/{self.view_resume_id}/careers/0/delete/")
        self.assertEqual(response.status_code, 302)
        profile = repository.read_profile(self.view_resume_id)
        self.assertEqual(len(profile["careers"]), 1)

    def test_education_add_and_delete_view(self):
        self.client.post(
            f"/resumes/{self.view_resume_id}/educations/add/", {"school_name": "테스트대학교"}
        )
        profile = repository.read_profile(self.view_resume_id)
        self.assertEqual(profile["educations"][-1]["school_name"], "테스트대학교")

        response = self.client.post(f"/resumes/{self.view_resume_id}/educations/1/delete/")
        self.assertEqual(response.status_code, 302)

    def test_confirm_view(self):
        response = self.client.post(f"/resumes/{self.view_resume_id}/confirm/")
        self.assertEqual(response.status_code, 302)
        profile = repository.read_profile(self.view_resume_id)
        self.assertTrue(profile["is_user_confirmed"])

    def test_reanalyze_view(self):
        response = self.client.post(f"/resumes/{self.view_resume_id}/reanalyze/")
        self.assertEqual(response.status_code, 302)
        document = repository.read_document(self.view_resume_id)
        self.assertIn(document["analysis_status"], ("COMPLETED", "COMPLETED_WITH_WARNINGS"))

    def test_detail_view_no_longer_has_raw_text_section_but_links_to_it(self):
        response = self.client.get(f"/resumes/{self.view_resume_id}/")
        self.assertEqual(response.status_code, 200)
        self.assertNotContains(response, 'id="raw-text"')
        self.assertContains(
            response, reverse("resumes:raw_text", args=[self.view_resume_id])
        )


class RawTextViewTests(ResumeEditingTestBase):
    def setUp(self):
        super().setUp()
        self.client = Client()
        self.user = signup_and_login(self.client)
        upload = SimpleUploadedFile("resume2.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.view_resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_raw_text_view_owner_200_renders_blocks(self):
        response = self.client.get(f"/resumes/{self.view_resume_id}/raw-text/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "홍길동")

    def test_raw_text_view_empty_blocks_shows_no_text_message(self):
        repository.write_blocks(self.view_resume_id, [])
        response = self.client.get(f"/resumes/{self.view_resume_id}/raw-text/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "추출된 텍스트가 없습니다")


class CareerDetailAccordionRenderTests(ResumeEditingTestBase):
    """detail.html의 <details class="career-detail"> 아코디언 기본 펼침(open)
    조건 — career.review_required 또는 sub_projects 중 review_required=True
    항목이 있으면 open, 둘 다 아니면 open 속성이 없어야 한다."""

    def setUp(self):
        super().setUp()
        self.client = Client()
        self.user = signup_and_login(self.client)
        upload = SimpleUploadedFile("resume3.pdf", build_text_resume_pdf(), content_type="application/pdf")
        self.view_resume_id = upload_service.upload_resume(self.user["id"], upload)

    def test_details_open_when_sub_project_review_required(self):
        profile = ResumeProfile(
            careers=[
                Career(
                    company_name_raw="ABC주식회사",
                    start_date="2020-01",
                    responsibilities=["- 결제 시스템 개발"],
                    sub_projects=[
                        Project(project_name="이력서 매칭 시스템 구축", review_required=True),
                    ],
                    review_required=False,
                )
            ]
        )
        repository.write_profile(self.view_resume_id, profile.model_dump())

        response = self.client.get(f"/resumes/{self.view_resume_id}/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, '<details class="career-detail" open>')

    def test_details_not_open_when_nothing_requires_review(self):
        profile = ResumeProfile(
            careers=[
                Career(
                    company_name_raw="ABC주식회사",
                    start_date="2020-01",
                    responsibilities=["- 결제 시스템 개발"],
                    sub_projects=[
                        Project(project_name="이력서 매칭 시스템 구축", review_required=False),
                    ],
                    review_required=False,
                )
            ]
        )
        repository.write_profile(self.view_resume_id, profile.model_dump())

        response = self.client.get(f"/resumes/{self.view_resume_id}/")

        self.assertEqual(response.status_code, 200)
        self.assertNotContains(response, '<details class="career-detail" open>')
        self.assertContains(response, '<details class="career-detail"')
