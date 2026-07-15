"""DART 고유번호 목록을 내려받아 corporations.json에 적재한다."""
from .. import repository
from ..clients.open_dart_client import OpenDartClient
from ..normalization import normalize_company_name


def sync_corporations(client=None) -> int:
    """corpCode.xml을 동기화하고 저장된 기업 수를 반환한다.

    DartError(키 없음/장애/Timeout 등)는 호출자에게 그대로 전파된다
    (management command / API 뷰에서 안내 메시지로 변환).
    """
    client = client or OpenDartClient()
    corporations = client.download_corp_codes()
    for corp in corporations:
        corp["normalized_name"] = normalize_company_name(corp.get("corp_name", ""))
    repository.write_corporations(corporations)
    return len(corporations)
