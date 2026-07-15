"""LLM 기반 이력서 파서 인터페이스 — 공급자 SDK 미결합 스텁.

SPEC.md 0장: "구조화: 규칙 기반 파서 기본. LLM 공급자 인터페이스(Protocol)만
정의, 실제 호출 구현은 스텁". 실제 LLM 호출은 이후 단계에서 공급자를
선택해 구현한다.
"""
from ..schemas.document import ExtractedDocument
from .base import ResumeParseResult


class LlmResumeParser:
    def parse(self, document: ExtractedDocument) -> ResumeParseResult:
        raise NotImplementedError(
            "LlmResumeParser는 아직 구현되지 않았습니다. RuleBasedResumeParser를 사용하세요."
        )
