"""이력서 구조화 결과(profile.json) 스키마 — SPEC.md 2장 기준.

basic 필드는 개별 항목이 ExtractedField로 감싸인다. careers/educations/
projects/certificates/skills/languages는 레코드 단위로 confidence/
source_page/source_text/review_required 1세트를 가진다.

민감정보(주민등록번호, 사진 등)는 필드 자체를 정의하지 않아 저장을 원천
차단한다.
"""
from pydantic import BaseModel, Field

from .common import DartMatchStatus, ExtractedField


class BasicInfo(BaseModel):
    name: ExtractedField[str] = Field(default_factory=ExtractedField)
    name_en: ExtractedField[str] = Field(default_factory=ExtractedField)
    birth_date: ExtractedField[str] = Field(default_factory=ExtractedField)
    email: ExtractedField[str] = Field(default_factory=ExtractedField)
    phone: ExtractedField[str] = Field(default_factory=ExtractedField)
    address: ExtractedField[str] = Field(default_factory=ExtractedField)
    portfolio_url: ExtractedField[str] = Field(default_factory=ExtractedField)
    github_url: ExtractedField[str] = Field(default_factory=ExtractedField)
    linkedin_url: ExtractedField[str] = Field(default_factory=ExtractedField)
    summary: ExtractedField[str] = Field(default_factory=ExtractedField)


class Project(BaseModel):
    project_name: str = ""
    organization: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    role: str | None = None
    description: str | None = None
    technologies: list[str] = Field(default_factory=list)
    achievements: list[str] = Field(default_factory=list)
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class Career(BaseModel):
    company_name_raw: str = ""
    company_name_normalized: str = ""
    department: str | None = None
    position: str | None = None
    employment_type: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    is_current: bool = False
    responsibilities: list[str] = Field(default_factory=list)
    achievements: list[str] = Field(default_factory=list)
    sub_projects: list[Project] = Field(default_factory=list)
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False
    dart_corp_code: str | None = None
    dart_match_status: DartMatchStatus = DartMatchStatus.NOT_SEARCHED


class Education(BaseModel):
    school_name: str = ""
    major: str | None = None
    degree: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    graduation_status: str | None = None
    description: str | None = None
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class Certificate(BaseModel):
    certificate_name: str = ""
    issuer: str | None = None
    acquired_date: str | None = None
    certificate_number: str | None = None
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class Skill(BaseModel):
    category: str | None = None
    name: str = ""
    level: str | None = None
    description: str | None = None
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class Language(BaseModel):
    language: str = ""
    test_name: str | None = None
    score: str | None = None
    grade: str | None = None
    acquired_date: str | None = None
    sort_order: int = 0
    confidence: float = 0.0
    source_page: int | None = None
    source_text: str | None = None
    review_required: bool = False


class ResumeProfile(BaseModel):
    basic: BasicInfo = Field(default_factory=BasicInfo)
    careers: list[Career] = Field(default_factory=list)
    educations: list[Education] = Field(default_factory=list)
    projects: list[Project] = Field(default_factory=list)
    certificates: list[Certificate] = Field(default_factory=list)
    skills: list[Skill] = Field(default_factory=list)
    languages: list[Language] = Field(default_factory=list)
    is_user_confirmed: bool = False
    confirmed_at: str | None = None
