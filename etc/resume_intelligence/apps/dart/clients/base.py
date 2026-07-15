from typing import Protocol

from .dto import CompanyProfileDTO, FinancialStatementDTO


class DartClient(Protocol):
    def is_configured(self) -> bool: ...

    def download_corp_codes(self) -> list[dict]: ...

    def get_company(self, corp_code: str) -> CompanyProfileDTO: ...

    def get_financial_accounts(
        self, corp_code: str, business_year: str, report_code: str, fs_div: str
    ) -> FinancialStatementDTO: ...
