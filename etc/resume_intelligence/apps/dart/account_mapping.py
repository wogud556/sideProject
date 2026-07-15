"""DART 단일회사 주요계정(fnlttSinglAcnt) 계정명 매핑 (SPEC.md 6장).

재무제표 작성 기준에 따라 동일한 의미의 계정이 다른 이름으로 보고되므로
(매출액/영업수익/수익(매출액) 등), 표준 6계정 이름으로 매핑하고 원본
계정명도 함께 보존한다.
"""
CANONICAL_ASSETS = "자산총계"
CANONICAL_LIABILITIES = "부채총계"
CANONICAL_EQUITY = "자본총계"
CANONICAL_REVENUE = "매출액"
CANONICAL_OPERATING_PROFIT = "영업이익"
CANONICAL_NET_INCOME = "당기순이익"

CANONICAL_ACCOUNTS = [
    CANONICAL_ASSETS,
    CANONICAL_LIABILITIES,
    CANONICAL_EQUITY,
    CANONICAL_REVENUE,
    CANONICAL_OPERATING_PROFIT,
    CANONICAL_NET_INCOME,
]

ACCOUNT_NAME_MAP: dict[str, str] = {
    "자산총계": CANONICAL_ASSETS,
    "부채총계": CANONICAL_LIABILITIES,
    "자본총계": CANONICAL_EQUITY,
    "매출액": CANONICAL_REVENUE,
    "영업수익": CANONICAL_REVENUE,
    "수익(매출액)": CANONICAL_REVENUE,
    "영업이익": CANONICAL_OPERATING_PROFIT,
    "영업이익(손실)": CANONICAL_OPERATING_PROFIT,
    "당기순이익": CANONICAL_NET_INCOME,
    "당기순이익(손실)": CANONICAL_NET_INCOME,
    "분기순이익": CANONICAL_NET_INCOME,
    "분기순이익(손실)": CANONICAL_NET_INCOME,
}


def map_account_name(raw_account_name: str | None) -> str | None:
    if not raw_account_name:
        return None
    return ACCOUNT_NAME_MAP.get(raw_account_name.strip())
