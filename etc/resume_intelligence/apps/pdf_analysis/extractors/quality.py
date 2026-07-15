"""페이지 텍스트 품질 평가 — OCR이 필요한 페이지를 판별한다."""
import re

import fitz

from ..schemas.document import PageQuality

MIN_CHAR_COUNT = 20
BROKEN_CHAR_RATIO_THRESHOLD = 0.1
IMAGE_AREA_RATIO_THRESHOLD = 0.5
IMAGE_BLOCK_TYPE = 1

_BROKEN_CHAR_RE = re.compile(r"[�\x00-\x08\x0b\x0c\x0e-\x1f]")


def assess_page_quality(page: fitz.Page, page_number: int) -> PageQuality:
    text = page.get_text()
    char_count = len(text.strip())

    broken_count = len(_BROKEN_CHAR_RE.findall(text))
    broken_char_ratio = (broken_count / len(text)) if text else 0.0

    page_area = page.rect.width * page.rect.height
    image_area = 0.0
    if page_area > 0:
        for block in page.get_text("dict").get("blocks", []):
            if block.get("type") == IMAGE_BLOCK_TYPE:
                bx0, by0, bx1, by1 = block["bbox"]
                image_area += max(0.0, bx1 - bx0) * max(0.0, by1 - by0)
    image_area_ratio = min(1.0, image_area / page_area) if page_area > 0 else 0.0

    needs_ocr = (
        char_count < MIN_CHAR_COUNT
        or broken_char_ratio > BROKEN_CHAR_RATIO_THRESHOLD
        or image_area_ratio > IMAGE_AREA_RATIO_THRESHOLD
    )

    return PageQuality(
        page=page_number,
        char_count=char_count,
        broken_char_ratio=round(broken_char_ratio, 4),
        image_area_ratio=round(image_area_ratio, 4),
        needs_ocr=needs_ocr,
    )
