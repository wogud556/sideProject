"""DartClient 응답 DTO."""
from dataclasses import dataclass, field


@dataclass
class CompanyProfileDTO:
    corp_code: str
    corp_name: str
    corp_name_eng: str | None
    stock_code: str | None
    ceo_name: str | None
    corp_cls: str | None
    address: str | None
    homepage_url: str | None
    est_date: str | None
    raw: dict = field(default_factory=dict)


@dataclass
class FinancialAccountDTO:
    canonical_name: str
    raw_account_name: str
    amount: str | None
    fs_div: str


@dataclass
class FinancialStatementDTO:
    corp_code: str
    business_year: str
    report_code: str
    fs_div: str
    accounts: list[FinancialAccountDTO] = field(default_factory=list)
    raw: list = field(default_factory=list)
