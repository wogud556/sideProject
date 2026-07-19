"""정규식/패턴 기반 이력서 구조화 파서 (기본 파서).

섹션 탐지(section_detector) → 섹션별 필드 추출(field_extractors) →
ExtractedField/레코드로 포장 → ResumeParseResult 반환.
"""
from ..schemas.common import ExtractedField
from ..schemas.document import ExtractedBlock, ExtractedDocument
from ..schemas.profile import BasicInfo, Career, Certificate, Education, Language, Project, ResumeProfile, Skill
from ..validators.field_validators import (
    detect_overlapping_periods,
    is_valid_date_ym,
    is_valid_email,
    is_valid_phone,
    validate_career_period,
)
from . import field_extractors as fx
from .base import ResumeParseResult
from .section_detector import SectionType, detect_sections

CONFIDENCE_HIGH = 0.9
CONFIDENCE_MEDIUM = 0.7
CONFIDENCE_LOW = 0.4
REVIEW_REQUIRED_THRESHOLD = 0.7

NAME_MAX_LENGTH = 10


def _review_required(confidence: float) -> bool:
    return confidence < REVIEW_REQUIRED_THRESHOLD


class RuleBasedResumeParser:
    def parse(self, document: ExtractedDocument) -> ResumeParseResult:
        warnings: list[str] = []
        sections = detect_sections(document)

        basic = self._parse_basic(sections[SectionType.BASIC])
        careers = self._parse_careers(sections[SectionType.CAREER])
        educations = self._parse_educations(sections[SectionType.EDUCATION])
        projects = self._parse_projects(sections[SectionType.PROJECT])
        certificates = self._parse_certificates(sections[SectionType.CERTIFICATE])
        skills = self._parse_skills(sections[SectionType.SKILL])
        languages = self._parse_languages(sections[SectionType.LANGUAGE])

        warnings.extend(self._validate_career_periods(careers))

        profile = ResumeProfile(
            basic=basic,
            careers=careers,
            educations=educations,
            projects=projects,
            certificates=certificates,
            skills=skills,
            languages=languages,
        )
        return ResumeParseResult(profile=profile, warnings=warnings)

    # ------------------------------------------------------------------
    # 기본정보
    # ------------------------------------------------------------------
    def _parse_basic(self, blocks: list[ExtractedBlock]) -> BasicInfo:
        basic = BasicInfo()

        for block in blocks:
            if basic.email.value is None:
                emails = fx.extract_emails(block.text)
                if emails:
                    value = emails[0]
                    confidence = CONFIDENCE_HIGH if is_valid_email(value) else CONFIDENCE_LOW
                    basic.email = ExtractedField[str](
                        value=value,
                        confidence=confidence,
                        source_page=block.page,
                        source_text=block.text,
                        review_required=_review_required(confidence),
                    )

            if basic.phone.value is None:
                phones = fx.extract_phones(block.text)
                if phones:
                    value = phones[0]
                    confidence = CONFIDENCE_HIGH if is_valid_phone(value) else CONFIDENCE_LOW
                    basic.phone = ExtractedField[str](
                        value=value,
                        confidence=confidence,
                        source_page=block.page,
                        source_text=block.text,
                        review_required=_review_required(confidence),
                    )

        if basic.name.value is None:
            self._extract_name(blocks, basic)

        return basic

    def _extract_name(self, blocks: list[ExtractedBlock], basic: BasicInfo) -> None:
        # 1순위: "성명:"/"이름:"/"Name:" 라벨이 붙은 라인
        for block in blocks:
            label_value = fx.extract_name_from_label(block.text)
            if label_value:
                basic.name = ExtractedField[str](
                    value=label_value,
                    confidence=CONFIDENCE_HIGH,
                    source_page=block.page,
                    source_text=block.text,
                    review_required=False,
                )
                return

        # 2순위: 문서 맨 앞의 짧은 텍스트(이메일/전화/URL이 아님) — 상단 대형 텍스트 관례
        for block in blocks:
            text = block.text.strip()
            if not text or len(text) > NAME_MAX_LENGTH:
                continue
            if fx.extract_emails(text) or fx.extract_phones(text):
                continue
            if fx.extract_date_range(text):
                continue
            basic.name = ExtractedField[str](
                value=text,
                confidence=CONFIDENCE_MEDIUM,
                source_page=block.page,
                source_text=block.text,
                review_required=_review_required(CONFIDENCE_MEDIUM),
            )
            return

    def _validate_career_periods(self, careers: list[Career]) -> list[str]:
        """SPEC.md 3장 검증 단계: 입사일≤퇴사일, 재직중 처리, 중복 기간 경고.

        각 경력에는 validate_career_period를 적용해 개별 기간 형식/역전
        오류를 warnings에 담고 해당 career.review_required를 True로
        올린다. 전체 경력에는 detect_overlapping_periods를 적용해 겹치는
        기간을 warnings에 추가한다(개별 review_required는 건드리지 않음 —
        어느 쪽이 잘못됐는지 특정할 수 없으므로).
        """
        warnings: list[str] = []

        for career in careers:
            period_warnings = validate_career_period(career.start_date, career.end_date, career.is_current)
            if period_warnings:
                warnings.extend(period_warnings)
                career.review_required = True

        overlap_warnings = detect_overlapping_periods(
            [(c.start_date, c.end_date, c.is_current) for c in careers]
        )
        warnings.extend(overlap_warnings)

        return warnings

    # ------------------------------------------------------------------
    # 경력 / 학력 / 프로젝트 — "헤더 라인 + 날짜범위 라인 + 상세 라인들" 그룹핑
    # ------------------------------------------------------------------
    def _group_entries(self, blocks: list[ExtractedBlock]) -> list[dict]:
        """헤더(다음 줄이 날짜범위)로 시작하는 엔트리 단위로 블록을 묶는다."""
        entries: list[dict] = []
        i = 0
        n = len(blocks)
        while i < n:
            header = blocks[i]
            has_next_date = i + 1 < n and fx.extract_date_range(blocks[i + 1].text) is not None
            header_is_date = fx.extract_date_range(header.text) is not None

            if has_next_date and not header_is_date:
                date_block = blocks[i + 1]
                date_range = fx.extract_date_range(date_block.text)
                j = i + 2
                detail_blocks: list[ExtractedBlock] = []
                while j < n:
                    next_has_date = j + 1 < n and fx.extract_date_range(blocks[j + 1].text) is not None
                    this_is_date = fx.extract_date_range(blocks[j].text) is not None
                    if next_has_date and not this_is_date:
                        break
                    detail_blocks.append(blocks[j])
                    j += 1
                entries.append(
                    {
                        "header": header,
                        "date_block": date_block,
                        "date_range": date_range,
                        "detail_blocks": detail_blocks,
                    }
                )
                i = j
            else:
                # 패턴에 맞지 않는 라인은 건너뛴다 (낮은 신뢰도로 다룰 정보 없음).
                i += 1

        return entries

    def _group_career_entries(self, blocks: list[ExtractedBlock]) -> list[dict]:
        """경력 전용 그룹핑. 회사 엔트리 안에 프로젝트 서브엔트리를 중첩시킨다."""
        entries: list[dict] = []
        i, n = 0, len(blocks)
        while i < n:
            header = blocks[i]
            has_next_date = i + 1 < n and fx.extract_date_range(blocks[i + 1].text) is not None
            header_is_date = fx.extract_date_range(header.text) is not None

            if has_next_date and not header_is_date:
                date_block = blocks[i + 1]
                date_range = fx.extract_date_range(date_block.text)
                j = i + 2
                detail_blocks: list[ExtractedBlock] = []
                sub_projects: list[dict] = []

                while j < n:
                    next_has_date = j + 1 < n and fx.extract_date_range(blocks[j + 1].text) is not None
                    this_is_date = fx.extract_date_range(blocks[j].text) is not None

                    if next_has_date and not this_is_date:
                        is_new_company = not detail_blocks or fx.contains_company_suffix(blocks[j].text)
                        if is_new_company:
                            break
                        sub_header = blocks[j]
                        sub_date_range = fx.extract_date_range(blocks[j + 1].text)
                        k = j + 2
                        sub_detail_blocks: list[ExtractedBlock] = []
                        while k < n:
                            k_next_has_date = k + 1 < n and fx.extract_date_range(blocks[k + 1].text) is not None
                            k_this_is_date = fx.extract_date_range(blocks[k].text) is not None
                            if k_next_has_date and not k_this_is_date:
                                break
                            sub_detail_blocks.append(blocks[k])
                            k += 1
                        sub_projects.append(
                            {"header": sub_header, "date_range": sub_date_range, "detail_blocks": sub_detail_blocks}
                        )
                        j = k
                        continue

                    detail_blocks.append(blocks[j])
                    j += 1

                entries.append(
                    {
                        "header": header,
                        "date_block": date_block,
                        "date_range": date_range,
                        "detail_blocks": detail_blocks,
                        "sub_projects": sub_projects,
                    }
                )
                i = j
            else:
                i += 1
        return entries

    def _parse_careers(self, blocks: list[ExtractedBlock]) -> list[Career]:
        careers: list[Career] = []
        for sort_order, entry in enumerate(self._group_career_entries(blocks)):
            header = entry["header"]
            start_date, end_date, is_current = entry["date_range"]
            responsibilities = [b.text for b in entry["detail_blocks"]]

            sub_projects = []
            for sp_order, sp in enumerate(entry["sub_projects"]):
                sp_start, sp_end, sp_current = sp["date_range"] or (None, None, False)
                sub_projects.append(
                    Project(
                        project_name=sp["header"].text,
                        start_date=sp_start,
                        end_date=sp_end,
                        achievements=[b.text for b in sp["detail_blocks"]],
                        sort_order=sp_order,
                        confidence=CONFIDENCE_LOW,
                        source_page=sp["header"].page,
                        source_text=sp["header"].text,
                        review_required=True,
                    )
                )

            confidence = CONFIDENCE_HIGH if fx.contains_company_suffix(header.text) else CONFIDENCE_MEDIUM

            careers.append(
                Career(
                    company_name_raw=header.text,
                    company_name_normalized=header.text,
                    start_date=start_date,
                    end_date=end_date,
                    is_current=is_current,
                    responsibilities=responsibilities,
                    sub_projects=sub_projects,
                    sort_order=sort_order,
                    confidence=confidence,
                    source_page=header.page,
                    source_text=header.text,
                    review_required=_review_required(confidence),
                )
            )
        return careers

    def _parse_educations(self, blocks: list[ExtractedBlock]) -> list[Education]:
        educations: list[Education] = []
        for sort_order, entry in enumerate(self._group_entries(blocks)):
            header = entry["header"]
            start_date, end_date, is_current = entry["date_range"]
            confidence = CONFIDENCE_MEDIUM

            educations.append(
                Education(
                    school_name=header.text,
                    start_date=start_date,
                    end_date=end_date,
                    graduation_status="재학중" if is_current else None,
                    sort_order=sort_order,
                    confidence=confidence,
                    source_page=header.page,
                    source_text=header.text,
                    review_required=_review_required(confidence),
                )
            )
        return educations

    def _parse_projects(self, blocks: list[ExtractedBlock]) -> list[Project]:
        projects: list[Project] = []
        for sort_order, entry in enumerate(self._group_entries(blocks)):
            header = entry["header"]
            start_date, end_date, is_current = entry["date_range"]
            achievements = [b.text for b in entry["detail_blocks"]]
            confidence = CONFIDENCE_MEDIUM

            projects.append(
                Project(
                    project_name=header.text,
                    start_date=start_date,
                    end_date=end_date,
                    achievements=achievements,
                    sort_order=sort_order,
                    confidence=confidence,
                    source_page=header.page,
                    source_text=header.text,
                    review_required=_review_required(confidence),
                )
            )
        return projects

    # ------------------------------------------------------------------
    # 자격증 / 어학 — 한 줄 = 한 레코드
    # ------------------------------------------------------------------
    def _parse_certificates(self, blocks: list[ExtractedBlock]) -> list[Certificate]:
        certificates: list[Certificate] = []
        for sort_order, block in enumerate(blocks):
            acquired_date = fx.extract_single_date(block.text)
            remainder = fx.DATE_TOKEN_RE.sub("", block.text).strip()
            tokens = remainder.split()

            if not tokens:
                continue

            name = tokens[0]
            issuer = " ".join(tokens[1:]) or None
            confidence = CONFIDENCE_MEDIUM if issuer and acquired_date else CONFIDENCE_LOW

            certificates.append(
                Certificate(
                    certificate_name=name,
                    issuer=issuer,
                    acquired_date=acquired_date,
                    sort_order=sort_order,
                    confidence=confidence,
                    source_page=block.page,
                    source_text=block.text,
                    review_required=_review_required(confidence),
                )
            )
        return certificates

    def _parse_languages(self, blocks: list[ExtractedBlock]) -> list[Language]:
        languages: list[Language] = []
        for sort_order, block in enumerate(blocks):
            acquired_date = fx.extract_single_date(block.text)
            remainder = fx.DATE_TOKEN_RE.sub("", block.text).strip()
            tokens = remainder.split()

            if not tokens:
                continue

            test_name = tokens[0]
            score = tokens[1] if len(tokens) > 1 else None
            language_name = fx.infer_language_from_test_name(test_name)
            confidence = CONFIDENCE_MEDIUM if language_name else CONFIDENCE_LOW

            languages.append(
                Language(
                    language=language_name or "",
                    test_name=test_name,
                    score=score,
                    acquired_date=acquired_date,
                    sort_order=sort_order,
                    confidence=confidence,
                    source_page=block.page,
                    source_text=block.text,
                    review_required=_review_required(confidence),
                )
            )
        return languages

    # ------------------------------------------------------------------
    # 기술 — 쉼표로 구분된 한 줄을 여러 레코드로 분해
    # ------------------------------------------------------------------
    def _parse_skills(self, blocks: list[ExtractedBlock]) -> list[Skill]:
        skills: list[Skill] = []
        sort_order = 0
        for block in blocks:
            for token in block.text.split(","):
                name = token.strip()
                if not name:
                    continue
                skills.append(
                    Skill(
                        name=name,
                        sort_order=sort_order,
                        confidence=CONFIDENCE_MEDIUM,
                        source_page=block.page,
                        source_text=block.text,
                        review_required=_review_required(CONFIDENCE_MEDIUM),
                    )
                )
                sort_order += 1
        return skills
