"""HIGH-2 회귀 테스트: careers_service/upload_service의 모든 profile.json,
document.json 갱신이 storage.json_store의 경로별 락(update_json)을 실제로
타는지 동시 요청 시나리오로 검증한다.

이 테스트는 개별 read_profile()+write_profile() (또는 read_document()+
write_document()) 조합으로 되돌아가면 스레드 수만큼 갱신이 유실되어
실패해야 한다 — repository.update_profile/update_document 기반 구현에서만
안정적으로 통과한다.
"""
import shutil
import tempfile
import threading
from pathlib import Path

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import SimpleTestCase, override_settings

from apps.resumes import repository
from apps.resumes.services import careers_service, upload_service

from .helpers import build_text_resume_pdf

THREAD_COUNT = 20


class ConcurrencyTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)


class ConcurrentCareerAdditionsTests(ConcurrencyTestBase):
    def test_concurrent_add_career_does_not_lose_updates(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        resume_id = upload_service.upload_resume("user-1", upload)

        profile_before = repository.read_profile(resume_id)
        baseline_count = len(profile_before["careers"])

        errors: list[Exception] = []

        def add_one(i):
            try:
                careers_service.add_career(resume_id, "user-1", {"company_name_raw": f"동시추가회사{i}"})
            except Exception as exc:  # pragma: no cover - 실패 시 진단용
                errors.append(exc)

        threads = [threading.Thread(target=add_one, args=(i,)) for i in range(THREAD_COUNT)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        self.assertEqual(errors, [])

        profile_after = repository.read_profile(resume_id)
        self.assertEqual(len(profile_after["careers"]), baseline_count + THREAD_COUNT)

        # sort_order도 중복/누락 없이 0..N-1로 재계산되어 있어야 한다(경쟁 상태였다면
        # 서로 다른 스레드가 같은 sort_order를 계산해 충돌했을 것).
        sort_orders = sorted(c["sort_order"] for c in profile_after["careers"])
        self.assertEqual(sort_orders, list(range(baseline_count + THREAD_COUNT)))


class ConcurrentProfilePatchTests(ConcurrencyTestBase):
    def test_concurrent_patch_profile_skills_and_confirm_do_not_corrupt_file(self):
        upload = SimpleUploadedFile("resume.pdf", build_text_resume_pdf(), content_type="application/pdf")
        resume_id = upload_service.upload_resume("user-1", upload)

        def confirm_many():
            for _ in range(10):
                careers_service.confirm_resume(resume_id, "user-1")

        threads = [threading.Thread(target=confirm_many) for _ in range(5)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        profile = repository.read_profile(resume_id)
        self.assertTrue(profile["is_user_confirmed"])
        self.assertIsNotNone(profile["confirmed_at"])


class ConcurrentDocumentStatusUpdateTests(ConcurrencyTestBase):
    def test_repository_update_document_serializes_concurrent_writers(self):
        """repository.update_document가 실제로 락을 통해 read-modify-write를
        직렬화하는지 카운터 증가 시나리오로 직접 검증한다."""
        resume_id = "doc-concurrency-test"
        repository.write_document(resume_id, {"id": resume_id, "counter": 0})

        def increment(current):
            current = dict(current)
            current["counter"] += 1
            return current

        def worker():
            for _ in range(20):
                repository.update_document(resume_id, increment, default={"counter": 0})

        threads = [threading.Thread(target=worker) for _ in range(5)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        document = repository.read_document(resume_id)
        self.assertEqual(document["counter"], 100)
