"""PDF 파일 경로 → (블록, 프로필) 구조화 파이프라인.

pdf_analysis는 순수 라이브러리다 — 결과를 data/ 디렉터리에 스스로 저장하지
않는다. 저장/오케스트레이션은 apps.resumes(Phase 4)에서 담당한다.
"""
from .extractors.base import PdfExtractor
from .extractors.hybrid_extractor import HybridExtractor
from .parsers.base import ResumeParser
from .parsers.rule_based_parser import RuleBasedResumeParser
from .parsers.section_detector import SectionType, exclude_section_blocks
from .schemas.document import AnalysisStatus, ExtractedBlock
from .schemas.profile import ResumeProfile


class PipelineResult:
    def __init__(
        self,
        blocks: list[ExtractedBlock],
        profile: ResumeProfile,
        ocr_used: bool,
        warnings: list[str],
        status: AnalysisStatus,
    ):
        self.blocks = blocks
        self.profile = profile
        self.ocr_used = ocr_used
        self.warnings = warnings
        self.status = status


def run_pipeline(
    file_path: str,
    extractor: PdfExtractor | None = None,
    parser: ResumeParser | None = None,
) -> PipelineResult:
    """PDF를 추출·구조화한다. 부분 실패 시에도 예외 대신 warnings로 보고한다."""
    extractor = extractor or HybridExtractor()
    parser = parser or RuleBasedResumeParser()

    document = extractor.extract(file_path)
    # 자기소개서 섹션은 구조화 대상이 아니므로 파싱 입력과 결과 블록 모두에서 제외한다.
    document.blocks = exclude_section_blocks(document.blocks, {SectionType.SELF_INTRO})
    warnings = list(document.warnings)

    parse_result = parser.parse(document)
    warnings.extend(parse_result.warnings)

    status = AnalysisStatus.COMPLETED_WITH_WARNINGS if warnings else AnalysisStatus.COMPLETED

    return PipelineResult(
        blocks=document.blocks,
        profile=parse_result.profile,
        ocr_used=document.ocr_used,
        warnings=warnings,
        status=status,
    )
