"""이력서 섹션(기본정보/경력/학력/프로젝트/자격증/기술/어학) 헤더 탐지.

블록을 순서대로 훑으며 섹션 헤더 라인을 만나면 이후 블록들을 해당 섹션으로
분류한다. 헤더를 만나기 전까지의 블록은 기본정보(BASIC) 섹션으로 취급한다.
"""
import re
from enum import Enum

from ..schemas.document import ExtractedBlock, ExtractedDocument


class SectionType(str, Enum):
    BASIC = "BASIC"
    CAREER = "CAREER"
    EDUCATION = "EDUCATION"
    PROJECT = "PROJECT"
    CERTIFICATE = "CERTIFICATE"
    SKILL = "SKILL"
    LANGUAGE = "LANGUAGE"


_SECTION_KEYWORDS: dict[SectionType, list[str]] = {
    SectionType.CAREER: ["경력", "경력사항", "경력 사항", "업무경력", "Work Experience", "Career", "Experience"],
    SectionType.EDUCATION: ["학력", "학력사항", "학력 사항", "Education"],
    SectionType.PROJECT: ["프로젝트", "프로젝트 경험", "Projects", "Project"],
    SectionType.CERTIFICATE: ["자격증", "자격 사항", "자격사항", "자격면허", "Certificates", "Certifications", "Certificate"],
    SectionType.SKILL: ["기술", "기술 스택", "기술스택", "보유기술", "Skills", "Tech Stack", "Technical Skills"],
    SectionType.LANGUAGE: ["어학", "어학 능력", "외국어", "Languages", "Language"],
}

MAX_HEADER_LENGTH = 20


def _normalize_header(text: str) -> str:
    return re.sub(r"\s+", "", text.strip().rstrip(":："))


_NORMALIZED_KEYWORDS: dict[SectionType, set[str]] = {
    section: {_normalize_header(kw).lower() for kw in keywords}
    for section, keywords in _SECTION_KEYWORDS.items()
}


def match_section_header(text: str) -> SectionType | None:
    normalized = _normalize_header(text)
    if not normalized or len(normalized) > MAX_HEADER_LENGTH:
        return None
    normalized_lower = normalized.lower()
    for section, keyword_set in _NORMALIZED_KEYWORDS.items():
        if normalized_lower in keyword_set:
            return section
    return None


def detect_sections(document: ExtractedDocument) -> dict[SectionType, list[ExtractedBlock]]:
    sections: dict[SectionType, list[ExtractedBlock]] = {section: [] for section in SectionType}
    current_section = SectionType.BASIC

    for block in sorted(document.blocks, key=lambda b: (b.page, b.order)):
        matched = match_section_header(block.text)
        if matched is not None:
            current_section = matched
            continue
        sections[current_section].append(block)

    return sections
