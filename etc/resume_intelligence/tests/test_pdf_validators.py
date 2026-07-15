from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import SimpleTestCase

from apps.pdf_analysis.validators.field_validators import (
    detect_overlapping_periods,
    is_valid_date_ym,
    is_valid_email,
    is_valid_phone,
    validate_career_period,
)
from apps.pdf_analysis.validators.file_validators import (
    MAX_FILE_SIZE_BYTES,
    PdfValidationError,
    validate_uploaded_pdf,
)

from .helpers import (
    build_empty_pdf,
    build_encrypted_pdf,
    build_multi_page_text_pdf,
    build_non_pdf_bytes,
    build_text_resume_pdf,
)


def _upload(content: bytes, name="resume.pdf", content_type="application/pdf"):
    return SimpleUploadedFile(name, content, content_type=content_type)


class ValidateUploadedPdfTests(SimpleTestCase):
    def test_valid_pdf_passes_and_returns_info(self):
        content = build_text_resume_pdf()
        info = validate_uploaded_pdf(_upload(content))
        self.assertEqual(info.page_count, 1)
        self.assertFalse(info.is_encrypted)
        self.assertEqual(info.file_size, len(content))
        self.assertEqual(len(info.file_hash), 64)

    def test_rejects_wrong_extension(self):
        content = build_text_resume_pdf()
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content, name="resume.txt"))
        self.assertEqual(ctx.exception.code, "INVALID_EXTENSION")

    def test_rejects_wrong_mime_type(self):
        content = build_text_resume_pdf()
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content, content_type="text/plain"))
        self.assertEqual(ctx.exception.code, "INVALID_MIME_TYPE")

    def test_rejects_non_pdf_signature(self):
        content = build_non_pdf_bytes()
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content))
        self.assertEqual(ctx.exception.code, "INVALID_PDF_SIGNATURE")

    def test_rejects_file_too_large(self):
        content = b"%PDF-1.7\n" + b"0" * (MAX_FILE_SIZE_BYTES + 1)
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content))
        self.assertEqual(ctx.exception.code, "FILE_TOO_LARGE")

    def test_accepts_file_exactly_at_size_limit(self):
        """경계값: 정확히 20MB(초과가 아님)인 유효한 PDF는 통과해야 한다."""
        base = build_text_resume_pdf()
        header_end = base.index(b"\n") + 1
        pad_len = MAX_FILE_SIZE_BYTES - len(base) - 2  # "%" + padding + "\n"
        padded = base[:header_end] + b"%" + b"0" * pad_len + b"\n" + base[header_end:]
        self.assertEqual(len(padded), MAX_FILE_SIZE_BYTES)

        info = validate_uploaded_pdf(_upload(padded))
        self.assertEqual(info.file_size, MAX_FILE_SIZE_BYTES)

    def test_rejects_too_many_pages(self):
        content = build_multi_page_text_pdf([["page"] for _ in range(31)])
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content))
        self.assertEqual(ctx.exception.code, "TOO_MANY_PAGES")

    def test_accepts_exactly_max_page_count(self):
        """경계값: 정확히 30페이지(초과가 아님)는 통과해야 한다."""
        content = build_multi_page_text_pdf([["page"] for _ in range(30)])
        info = validate_uploaded_pdf(_upload(content))
        self.assertEqual(info.page_count, 30)

    def test_rejects_encrypted_pdf(self):
        content = build_encrypted_pdf()
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content))
        self.assertEqual(ctx.exception.code, "ENCRYPTED_PDF")

    def test_rejects_empty_pdf(self):
        content = build_empty_pdf()
        with self.assertRaises(PdfValidationError) as ctx:
            validate_uploaded_pdf(_upload(content))
        self.assertEqual(ctx.exception.code, "EMPTY_PDF")

    def test_rejects_corrupted_pdf(self):
        content = b"%PDF-1.7\n%broken garbage that is not a real pdf structure"
        with self.assertRaises(PdfValidationError):
            validate_uploaded_pdf(_upload(content))

    def test_cursor_reset_after_validation(self):
        content = build_text_resume_pdf()
        upload = _upload(content)
        validate_uploaded_pdf(upload)
        self.assertEqual(upload.read(), content)


class EmailPhoneDateValidatorTests(SimpleTestCase):
    def test_valid_emails(self):
        for value in ["hong@example.com", "hong.gildong+resume@sub.example.co.kr"]:
            self.assertTrue(is_valid_email(value), value)

    def test_invalid_emails(self):
        for value in [None, "", "not-an-email", "hong@", "@example.com"]:
            self.assertFalse(is_valid_email(value), value)

    def test_valid_phones(self):
        for value in ["010-1234-5678", "01012345678", "02-1234-5678", "+82-10-1234-5678"]:
            self.assertTrue(is_valid_phone(value), value)

    def test_invalid_phones(self):
        for value in [None, "", "123", "010-abcd-5678"]:
            self.assertFalse(is_valid_phone(value), value)

    def test_valid_date_ym(self):
        self.assertTrue(is_valid_date_ym("2020-03"))
        self.assertTrue(is_valid_date_ym("2020-12"))

    def test_invalid_date_ym(self):
        for value in [None, "", "2020-13", "2020/03", "20-03"]:
            self.assertFalse(is_valid_date_ym(value), value)


class CareerPeriodValidationTests(SimpleTestCase):
    def test_valid_period_no_warnings(self):
        warnings = validate_career_period("2020-03", "2022-05", is_current=False)
        self.assertEqual(warnings, [])

    def test_current_job_with_end_date_warns(self):
        warnings = validate_career_period("2020-03", "2022-05", is_current=True)
        self.assertTrue(any("재직중" in w for w in warnings))

    def test_start_after_end_warns(self):
        warnings = validate_career_period("2022-05", "2020-03", is_current=False)
        self.assertTrue(any("늦습니다" in w for w in warnings))

    def test_invalid_format_warns(self):
        warnings = validate_career_period("2020.03", None, is_current=False)
        self.assertTrue(any("형식" in w for w in warnings))

    def test_current_job_without_end_date_no_warning(self):
        warnings = validate_career_period("2020-03", None, is_current=True)
        self.assertEqual(warnings, [])


class OverlappingPeriodsTests(SimpleTestCase):
    def test_no_overlap(self):
        periods = [("2018-01", "2019-12", False), ("2020-01", "2021-12", False)]
        self.assertEqual(detect_overlapping_periods(periods), [])

    def test_overlap_detected(self):
        periods = [("2018-01", "2020-06", False), ("2020-01", "2021-12", False)]
        warnings = detect_overlapping_periods(periods)
        self.assertEqual(len(warnings), 1)

    def test_current_job_overlap_with_later_start(self):
        periods = [("2018-01", None, True), ("2022-01", "2022-12", False)]
        warnings = detect_overlapping_periods(periods)
        self.assertEqual(len(warnings), 1)
