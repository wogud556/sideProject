from typing import Protocol

from ..schemas.document import ExtractedDocument
from ..schemas.profile import ResumeProfile


class ResumeParseResult:
    """ResumeParser.parse()의 반환값. profile과 파싱 중 발생한 경고를 담는다."""

    def __init__(self, profile: ResumeProfile, warnings: list[str] | None = None):
        self.profile = profile
        self.warnings = warnings or []


class ResumeParser(Protocol):
    def parse(self, document: ExtractedDocument) -> ResumeParseResult: ...
