"""OpenDART Open API 클라이언트 (requests 기반).

DART_API_KEY 환경변수(settings.DART_API_KEY)를 사용한다. 키가 없으면
is_configured()가 False를 반환하며, 호출 시 DartApiKeyMissing을 발생시킨다.
응답 원본(raw)은 DTO에 함께 담아 호출부(services)가 캐시 파일에 보존할 수
있게 한다.
"""
import io
import xml.etree.ElementTree as ET
import zipfile

import requests
from django.conf import settings

from .. import constants, exceptions
from ..account_mapping import map_account_name
from .dto import CompanyProfileDTO, FinancialAccountDTO, FinancialStatementDTO


class OpenDartClient:
    def __init__(
        self,
        api_key: str | None = None,
        base_url: str = constants.DART_API_BASE_URL,
        timeout: int = constants.REQUEST_TIMEOUT_SECONDS,
    ):
        self._api_key = api_key if api_key is not None else getattr(settings, "DART_API_KEY", "")
        self._base_url = base_url
        self._timeout = timeout

    def is_configured(self) -> bool:
        return bool(self._api_key)

    def _require_key(self) -> None:
        if not self.is_configured():
            raise exceptions.DartApiKeyMissing("DART_API_KEY가 설정되지 않았습니다.")

    def _raise_for_status(self, data: dict) -> None:
        status = data.get("status")
        if status == constants.STATUS_SUCCESS:
            return
        message = data.get("message", "")
        if status == constants.STATUS_NO_DATA:
            raise exceptions.DartNotFound(message or "조회된 데이터가 없습니다.")
        if status == constants.STATUS_INVALID_KEY:
            raise exceptions.DartApiKeyMissing(message or "등록되지 않은 키입니다.")
        if status == constants.STATUS_RATE_LIMITED:
            raise exceptions.DartRateLimited(message or "API 사용 한도를 초과했습니다.")
        if status == constants.STATUS_SYSTEM_ERROR:
            raise exceptions.DartApiError(message or "DART 시스템 점검 중입니다.")
        if status == constants.STATUS_UNDEFINED_ERROR:
            raise exceptions.DartTimeout(message or "정의되지 않은 오류가 발생했습니다.")
        raise exceptions.DartApiError(message or f"알 수 없는 상태 코드: {status}")

    def _get_json(self, path: str, params: dict) -> dict:
        self._require_key()
        request_params = {**params, "crtfc_key": self._api_key}
        try:
            response = requests.get(f"{self._base_url}/{path}", params=request_params, timeout=self._timeout)
            response.raise_for_status()
        except requests.exceptions.Timeout as exc:
            raise exceptions.DartTimeout("DART API 요청이 시간 초과되었습니다.") from exc
        except requests.exceptions.RequestException as exc:
            # response.raise_for_status()가 던지는 HTTPError도 RequestException의
            # 하위 클래스이므로 여기서 함께 DartApiError로 매핑된다.
            raise exceptions.DartApiError("DART API 요청 중 오류가 발생했습니다.") from exc

        try:
            data = response.json()
        except ValueError as exc:
            # response.json()은 비-JSON 본문에서 json.JSONDecodeError(ValueError의
            # 하위 클래스)를 던진다 — 그대로 흘리면 화면이 500으로 죽으므로
            # DartApiError로 변환해 graceful degrade 경로를 타게 한다.
            raise exceptions.DartApiError("DART API 응답을 해석할 수 없습니다.") from exc

        self._raise_for_status(data)
        return data

    def download_corp_codes(self) -> list[dict]:
        """corpCode.xml(ZIP)을 내려받아 [{corp_code,corp_name,corp_eng_name,stock_code,modify_date}] 반환."""
        self._require_key()
        try:
            response = requests.get(
                f"{self._base_url}/corpCode.xml",
                params={"crtfc_key": self._api_key},
                timeout=self._timeout,
            )
        except requests.exceptions.Timeout as exc:
            raise exceptions.DartTimeout("DART 고유번호 다운로드가 시간 초과되었습니다.") from exc
        except requests.exceptions.RequestException as exc:
            raise exceptions.DartApiError("DART 고유번호 다운로드 중 오류가 발생했습니다.") from exc

        if response.content[:2] != b"PK":
            # ZIP이 아니면 오류 응답(XML)일 가능성이 높다.
            try:
                root = ET.fromstring(response.content)
                self._raise_for_status(
                    {"status": root.findtext("status"), "message": root.findtext("message", "")}
                )
            except ET.ParseError:
                pass
            raise exceptions.DartApiError("고유번호 파일 형식이 올바르지 않습니다.")

        with zipfile.ZipFile(io.BytesIO(response.content)) as zf:
            xml_bytes = zf.read("CORPCODE.xml")

        return self._parse_corp_code_xml(xml_bytes)

    @staticmethod
    def _parse_corp_code_xml(xml_bytes: bytes) -> list[dict]:
        root = ET.fromstring(xml_bytes)
        corporations = []
        for item in root.findall("list"):
            corporations.append(
                {
                    "corp_code": (item.findtext("corp_code") or "").strip(),
                    "corp_name": (item.findtext("corp_name") or "").strip(),
                    "corp_eng_name": (item.findtext("corp_eng_name") or "").strip(),
                    "stock_code": (item.findtext("stock_code") or "").strip(),
                    "modify_date": (item.findtext("modify_date") or "").strip(),
                }
            )
        return corporations

    def get_company(self, corp_code: str) -> CompanyProfileDTO:
        data = self._get_json("company.json", {"corp_code": corp_code})
        return CompanyProfileDTO(
            corp_code=data.get("corp_code", corp_code),
            corp_name=data.get("corp_name", ""),
            corp_name_eng=data.get("corp_name_eng"),
            stock_code=data.get("stock_code"),
            ceo_name=data.get("ceo_nm"),
            corp_cls=data.get("corp_cls"),
            address=data.get("adres"),
            homepage_url=data.get("hm_url"),
            est_date=data.get("est_dt"),
            raw=data,
        )

    def get_financial_accounts(
        self, corp_code: str, business_year: str, report_code: str, fs_div: str
    ) -> FinancialStatementDTO:
        data = self._get_json(
            "fnlttSinglAcnt.json",
            {
                "corp_code": corp_code,
                "bsns_year": business_year,
                "reprt_code": report_code,
                "fs_div": fs_div,
            },
        )
        raw_list = data.get("list", [])

        accounts: list[FinancialAccountDTO] = []
        seen: set[str] = set()
        for row in raw_list:
            canonical = map_account_name(row.get("account_nm", ""))
            if canonical is None or canonical in seen:
                continue
            seen.add(canonical)
            accounts.append(
                FinancialAccountDTO(
                    canonical_name=canonical,
                    raw_account_name=row.get("account_nm", ""),
                    amount=row.get("thstrm_amount"),
                    fs_div=row.get("fs_div", fs_div),
                )
            )

        return FinancialStatementDTO(
            corp_code=corp_code,
            business_year=business_year,
            report_code=report_code,
            fs_div=fs_div,
            accounts=accounts,
            raw=raw_list,
        )
