"""OpenDART API 상수."""

DART_API_BASE_URL = "https://opendart.fss.or.kr/api"
REQUEST_TIMEOUT_SECONDS = 10

STATUS_SUCCESS = "000"
STATUS_NO_DATA = "013"
STATUS_INVALID_KEY = "010"
STATUS_RATE_LIMITED = "020"
STATUS_SYSTEM_ERROR = "800"
STATUS_UNDEFINED_ERROR = "900"

# 사업보고서(연간) 기준. 반기/분기 보고서 코드도 참고용으로 남겨둔다.
REPORT_CODE_ANNUAL = "11011"
REPORT_CODE_HALF = "11012"
REPORT_CODE_Q3 = "11014"
REPORT_CODE_Q1 = "11013"

FS_DIV_CONSOLIDATED = "CFS"  # 연결재무제표 (우선)
FS_DIV_SEPARATE = "OFS"  # 별도재무제표 (CFS 없을 때 폴백)

CORP_CODE_PATTERN = r"^[0-9]{8}$"

# 재무정보 조회 쿼리 파라미터 화이트리스트 (MED-6) — year/report_code는
# 캐시 파일명 조합에 그대로 쓰이므로 반드시 검증 후에만 사용한다.
YEAR_PATTERN = r"^\d{4}$"
VALID_REPORT_CODES = frozenset({REPORT_CODE_ANNUAL, REPORT_CODE_HALF, REPORT_CODE_Q1, REPORT_CODE_Q3})
