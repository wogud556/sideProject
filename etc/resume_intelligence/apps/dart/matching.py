"""경력 기업명 ↔ DART 기업 매칭 (SPEC.md 6장).

순서: 정규화 완전일치 → 정식명 완전일치 → 부분 문자열 → 유사도(difflib).
완전일치 1건이면 자동 연결 후보(MATCHED_AUTO), 복수면 MULTIPLE_CANDIDATES로
사용자 선택, 없으면 NOT_FOUND. 단, 정규화명이 2자 이하인 짧은 이름은
자동 연결을 금지한다(MULTIPLE_CANDIDATES로 낮춰 수동 확인을 유도).
"""
import difflib
from typing import Any, TypedDict

from apps.pdf_analysis.schemas.common import DartMatchStatus

from .normalization import normalize_company_name

SHORT_NAME_MAX_LENGTH = 2
SIMILARITY_THRESHOLD = 0.6
MAX_SIMILARITY_CANDIDATES = 5


class MatchCandidate(TypedDict):
    corp_code: str
    corp_name: str
    stock_code: str
    similarity: float


class MatchResult(TypedDict):
    status: DartMatchStatus
    candidates: list[MatchCandidate]


def _to_candidate(corp: dict[str, Any], similarity: float = 1.0) -> MatchCandidate:
    return {
        "corp_code": corp.get("corp_code", ""),
        "corp_name": corp.get("corp_name", ""),
        "stock_code": corp.get("stock_code", ""),
        "similarity": round(similarity, 4),
    }


def match_company(company_name_raw: str, corporations: list[dict[str, Any]]) -> MatchResult:
    normalized_query = normalize_company_name(company_name_raw)

    if not normalized_query:
        return {"status": DartMatchStatus.NOT_FOUND, "candidates": []}

    candidates: list[MatchCandidate] = []

    # 1) 정규화 완전일치
    exact = [c for c in corporations if c.get("normalized_name") == normalized_query]
    if exact:
        candidates = [_to_candidate(c) for c in exact]
    else:
        # 2) 정식명 완전일치 (원본 그대로)
        raw_query = company_name_raw.strip()
        exact_raw = [c for c in corporations if c.get("corp_name", "").strip() == raw_query]
        if exact_raw:
            candidates = [_to_candidate(c) for c in exact_raw]
        else:
            # 3) 부분 문자열
            substring = [
                c
                for c in corporations
                if c.get("normalized_name")
                and (normalized_query in c["normalized_name"] or c["normalized_name"] in normalized_query)
            ]
            if substring:
                candidates = [_to_candidate(c) for c in substring]
            else:
                # 4) 유사도(difflib)
                scored = []
                for c in corporations:
                    target = c.get("normalized_name", "")
                    if not target:
                        continue
                    ratio = difflib.SequenceMatcher(None, normalized_query, target).ratio()
                    if ratio >= SIMILARITY_THRESHOLD:
                        scored.append((ratio, c))
                scored.sort(key=lambda pair: pair[0], reverse=True)
                candidates = [_to_candidate(c, ratio) for ratio, c in scored[:MAX_SIMILARITY_CANDIDATES]]

    if not candidates:
        return {"status": DartMatchStatus.NOT_FOUND, "candidates": []}

    if len(candidates) == 1:
        if len(normalized_query) <= SHORT_NAME_MAX_LENGTH:
            # 이름이 너무 짧아 오탐 위험이 크므로 자동 연결하지 않는다.
            return {"status": DartMatchStatus.MULTIPLE_CANDIDATES, "candidates": candidates}
        return {"status": DartMatchStatus.MATCHED_AUTO, "candidates": candidates}

    return {"status": DartMatchStatus.MULTIPLE_CANDIDATES, "candidates": candidates}
