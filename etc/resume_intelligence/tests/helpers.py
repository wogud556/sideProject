"""테스트용 합성 PDF 및 DART 응답 fixture 로딩 헬퍼.

실제 스캔본/샘플 이력서 없이 텍스트 PDF / 이미지 전용 페이지 PDF / 암호화
PDF / 빈 PDF를 즉석에서 만들어 반환한다. 모든 PDF 생성 함수는 bytes를
반환한다. DART 관련 fixture는 tests/fixtures/에서 읽는다(실제 API 호출 금지).
"""
import io
import json
import zipfile
from pathlib import Path
from typing import Any

import fitz
from PIL import Image, ImageDraw

FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures"


def signup_and_login(client, username="tester", email=None, password="testpass123") -> dict[str, Any]:
    """apps.accounts를 통해 사용자를 생성하고 Django test Client 세션에 로그인한다."""
    from apps.accounts import services as account_services

    email = email or f"{username}@example.com"
    user = account_services.signup(username, email, password)
    response = client.post("/accounts/login/", {"username": username, "password": password})
    assert response.status_code == 302, f"로그인 실패: {response.status_code}"
    return user

# 한글 글리프를 지원하는 PyMuPDF 내장 CJK 폰트 (별도 폰트 파일 불필요).
KOREAN_FONT = "korea-s"

SAMPLE_RESUME_LINES = [
    "홍길동",
    "email: hong.gildong@example.com",
    "phone: 010-1234-5678",
    "",
    "경력",
    "ABC주식회사 백엔드 개발자",
    "2020.03 ~ 2022.05",
    "- 결제 시스템 개발",
    "",
    "(주)한아티 서버 개발자",
    "2022.06 ~ 재직중",
    "- 이력서 분석 서비스 개발",
    "",
    "학력",
    "한국대학교 컴퓨터공학과 학사",
    "2016.03 ~ 2020.02",
    "",
    "프로젝트",
    "이력서 분석기 개발",
    "2023.01 ~ 2023.06",
    "",
    "자격증",
    "정보처리기사 한국산업인력공단 2021.11",
    "",
    "기술",
    "Python, Django, PostgreSQL",
    "",
    "어학",
    "TOEIC 900 2020.05",
]


def build_text_resume_pdf(lines: list[str] | None = None) -> bytes:
    """텍스트 레이어가 있는 합성 이력서 PDF(1페이지)를 생성한다."""
    lines = lines if lines is not None else SAMPLE_RESUME_LINES
    doc = fitz.open()
    page = doc.new_page(width=595, height=842)  # A4
    y = 60
    # 이름을 문서 상단에 큰 글씨로 배치 (이름 추출 규칙 대상)
    page.insert_text((72, y), lines[0], fontsize=20, fontname=KOREAN_FONT)
    y += 40
    for line in lines[1:]:
        page.insert_text((72, y), line, fontsize=11, fontname=KOREAN_FONT)
        y += 20
    data = doc.tobytes()
    doc.close()
    return data


def build_multi_page_text_pdf(pages_lines: list[list[str]]) -> bytes:
    """여러 페이지로 구성된 텍스트 PDF를 생성한다."""
    doc = fitz.open()
    for page_lines in pages_lines:
        page = doc.new_page(width=595, height=842)
        y = 72
        for line in page_lines:
            page.insert_text((72, y), line, fontsize=11, fontname=KOREAN_FONT)
            y += 20
    data = doc.tobytes()
    doc.close()
    return data


def build_image_only_pdf(text_lines: list[str] | None = None) -> bytes:
    """텍스트 레이어 없이 이미지로만 구성된 페이지 PDF(OCR 필요)를 생성한다."""
    text_lines = text_lines or ["HONG GILDONG", "email: hong@example.com", "phone: 010-1234-5678"]
    width, height = 800, 400
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    y = 20
    for line in text_lines:
        draw.text((20, y), line, fill="black")
        y += 40
    buf = io.BytesIO()
    image.save(buf, format="PNG")
    image_bytes = buf.getvalue()

    doc = fitz.open()
    page = doc.new_page(width=width, height=height)
    page.insert_image(page.rect, stream=image_bytes)
    data = doc.tobytes()
    doc.close()
    return data


def build_encrypted_pdf(user_password: str = "user123", owner_password: str = "owner123") -> bytes:
    """비밀번호로 암호화된 PDF를 생성한다."""
    doc = fitz.open()
    page = doc.new_page(width=595, height=842)
    page.insert_text((72, 72), "암호화된 이력서")
    permissions = int(fitz.PDF_PERM_ACCESSIBILITY | fitz.PDF_PERM_PRINT)
    data = doc.tobytes(
        encryption=fitz.PDF_ENCRYPT_AES_256,
        owner_pw=owner_password,
        user_pw=user_password,
        permissions=permissions,
    )
    doc.close()
    return data


def build_empty_pdf() -> bytes:
    """텍스트도 이미지도 없는 빈 페이지로만 구성된 PDF를 생성한다.

    (PyMuPDF는 0페이지 문서를 저장할 수 없어 "완전히 빈 내용의 1페이지"로
    "빈 PDF"를 표현한다.)
    """
    doc = fitz.open()
    doc.new_page(width=595, height=842)
    data = doc.tobytes()
    doc.close()
    return data


def build_non_pdf_bytes() -> bytes:
    """PDF 시그니처가 없는 임의의 바이트."""
    return b"this is not a pdf file, just plain text bytes"


# ----------------------------------------------------------------------
# DART fixture 로딩 (실제 API 호출 없이 mock 응답으로 사용)
# ----------------------------------------------------------------------
def load_fixture_json(name: str) -> dict:
    return json.loads((FIXTURES_DIR / name).read_text(encoding="utf-8"))


def load_fixture_bytes(name: str) -> bytes:
    return (FIXTURES_DIR / name).read_bytes()


def build_corp_code_zip_bytes(xml_fixture_name: str = "dart_corp_list_sample.xml") -> bytes:
    """dart_corp_list_sample.xml을 CORPCODE.xml이라는 이름으로 담은 ZIP bytes를 만든다
    (실제 corpCode.xml API가 ZIP으로 응답하는 것과 동일한 형태)."""
    xml_bytes = load_fixture_bytes(xml_fixture_name)
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as zf:
        zf.writestr("CORPCODE.xml", xml_bytes)
    return buffer.getvalue()
