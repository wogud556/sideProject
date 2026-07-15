"""OpenDART 연동 예외.

DART 오류(키 없음/한도 초과/미검색/장애/Timeout)가 이력서 기능에 영향을
주면 안 되므로(SPEC.md 6장), 호출부는 항상 DartError를 잡아 안내 응답으로
변환해야 한다.
"""


class DartError(Exception):
    """모든 DART 연동 오류의 공통 베이스."""


class DartApiKeyMissing(DartError):
    """DART_API_KEY가 설정되지 않았거나(status 010) 등록되지 않은 키."""


class DartNotFound(DartError):
    """조회된 데이터가 없음 (status 013)."""


class DartRateLimited(DartError):
    """API 사용 한도 초과 (status 020)."""


class DartApiError(DartError):
    """DART 시스템 오류 등 그 외 오류 (status 800, 네트워크 오류 등)."""


class DartTimeout(DartError):
    """요청 타임아웃 또는 정의되지 않은 오류 (status 900)."""


class DartInvalidQuery(DartError):
    """year/report_code 등 요청 파라미터가 화이트리스트를 벗어남 (MED-6).

    캐시 파일명 조합에 쓰이기 전에 검증하기 위한 전용 예외 — DartError의
    하위 클래스이므로 기존 "DART 오류는 이력서 기능에 영향 없음/화면에서
    안내만" 처리 경로를 그대로 탄다.
    """
