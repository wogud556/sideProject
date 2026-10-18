import axios from 'axios'
import api from './axios'

// ─── 타입 ───────────────────────────────────────────────

export type OperatorRole = 'OPERATIONS' | 'UNDERWRITER' | 'APPROVER'
export type ApplicationStatus =
  | 'APPLIED' | 'COLLATERAL_EVALUATED' | 'LIMIT_CALCULATED' | 'DOCUMENT_REQUIRED'
  | 'SCREENING_APPROVED' | 'CONDITIONALLY_APPROVED' | 'REJECTED' | 'APPROVED'
  | 'AGREED' | 'EXECUTION_READY' | 'EXECUTED' | 'WITHDRAWN'
export type LienType = 'SENIOR_MORTGAGE' | 'LEASE_DEPOSIT' | 'OWN_MORTGAGE'
export type ValuationMethod = 'MARKET_PRICE' | 'APPRAISAL'
export type PolicyType = 'LTV' | 'DTI' | 'DSR' | 'STRESS_RATE' | 'MAX_LOAN_AMOUNT'
export type AreaResult = 'PASS' | 'HOLD' | 'FAIL'

export interface Operator { operatorId: string; operatorName: string; role: OperatorRole }

export interface ApplicationSummary {
  applicationId: number
  applicationNo: string
  customerId: string
  customerName: string
  productCode: string
  requestedAmount: number
  status: ApplicationStatus
  statusLabel: string
  appliedAt: string
  updatedAt: string
}

export interface Property {
  address: string; regionCode: string; propertyType: string; exclusiveArea: number | null; declaredPrice: number
}
export interface Income { incomeType: string; annualIncome: number; employerName: string | null }
export interface Debt {
  debtId: number; debtType: string; institution: string | null; balance: number; annualPrincipal: number; annualInterest: number
}
export interface Valuation {
  valuationId: number; valuationMethod: ValuationMethod; appraisedValue: number; valuationDate: string; valuedBy: string
}
export interface Lien {
  lienId: number; lienType: LienType; creditor: string; amount: number; lienRank: number; registeredBy: string
}

export interface LimitResult {
  limitResultId: number
  baseDate: string
  regionCode: string
  borrowerType: string
  creditScore: number
  appliedRate: number
  collateralValue: number
  seniorClaims: number
  annualIncome: number
  existingDebtService: number
  requestedAmount: number
  ltvPercent: number; dtiPercent: number; dsrPercent: number; stressRate: number
  ltvPolicyId: number; dtiPolicyId: number; dsrPolicyId: number; stressPolicyId: number; maxAmountPolicyId: number
  ltvLimit: number; collateralLimit: number; dtiLimit: number; dsrLimit: number; stressDsrLimit: number
  productLimit: number; regulatoryLimit: number; finalLimit: number
  bindingConstraint: 'COLLATERAL' | 'DTI' | 'STRESS_DSR' | 'PRODUCT' | 'REGULATORY'
  requestedLtv: number; requestedDti: number; requestedDsr: number; requestedStressDsr: number
  calculatedBy: string
  calculatedAt: string
}

export interface Screening {
  screeningId: number
  limitResultId: number
  status: string
  statusLabel: string
  borrowerResult: AreaResult
  collateralResult: AreaResult
  regulatoryResult: AreaResult
  reasons: { code: string; message: string }[]
  recommendedAmount: number | null
  requiresPreExecutionCheck: boolean
  incomeVerified: boolean
  ownershipVerified: boolean
  screenerId: string
  screeningComment: string | null
  screenedAt: string
}

export interface Approval {
  approvedAmount: number; interestRate: number; termMonths: number; repaymentMethod: string
  conditions: string | null; conditionsFulfilled: boolean; conditionsConfirmedBy: string | null
  approverId: string; approvalComment: string | null; approvedAt: string; expiresOn: string
}

export interface Agreement {
  agreementNo: string; disbursementAccountNo: string; agreedAmount: number; interestRate: number
  agreedAt: string; lienId: number | null; lienConfirmedBy: string | null; lienConfirmedAt: string | null
}

export interface Execution {
  executionId: number; executionNo: string; idempotencyKey: string; status: 'COMPLETED' | 'FAILED'
  amount: number; disbursementAccountNo: string; bankTransactionNo: string | null; failureReason: string | null
  loanAccountId: number | null; executedBy: string; executedAt: string
}

export interface ApplicationDetail {
  application: ApplicationSummary
  repaymentMethod: string
  termMonths: number
  loanPurpose: string
  houseCount: number
  firstHomeBuyer: boolean
  borrowerType: string
  property: Property
  income: Income
  debts: Debt[]
  valuations: Valuation[]
  liens: Lien[]
  limit: LimitResult | null
  screenings: Screening[]
  approval: Approval | null
  agreement: Agreement | null
  executions: Execution[]
  loanAccountId: number | null
}

export interface Policy {
  policyId: number; policyType: PolicyType; regionCode: string; borrowerType: string; productCode: string
  policyValue: number; effectiveFrom: string; effectiveTo: string | null; policyVersion: number
  basis: string | null; createdBy: string
}

export interface PolicyCreateRequest {
  policyType: PolicyType; regionCode: string; borrowerType: string; productCode: string
  policyValue: number; effectiveFrom: string; effectiveTo?: string; basis?: string
}

export interface Installment {
  installmentNo: number; dueDate: string; principal: number; interest: number; totalPayment: number
  balanceAfter: number; status: 'SCHEDULED' | 'PAID'; paidAt: string | null
}

export interface LoanAccountResponse {
  account: {
    loanAccountId: number; loanAccountNo: string; customerId: string; productCode: string; principal: number
    outstandingPrincipal: number; interestRate: number; termMonths: number; repaymentMethod: string
    executionDate: string; maturityDate: string; status: string
  }
  statusLabel: string
  repaymentMethodLabel: string
  nextInstallment: Installment | null
}

export interface RepaymentHistory {
  repaymentId: number; installmentNo: number; principalPaid: number; interestPaid: number; totalPaid: number
  outstandingAfter: number; sourceAccountNo: string; idempotencyKey: string; paidAt: string
}

// ─── API ────────────────────────────────────────────────

export const getOperators = () => api.get<Operator[]>('/operators')
export const getApplications = (status?: ApplicationStatus) =>
  api.get<ApplicationSummary[]>('/applications', { params: { status } })
export const getApplication = (id: number) => api.get<ApplicationDetail>(`/applications/${id}`)

export const evaluateCollateral = (id: number, body: {
  valuationMethod: ValuationMethod
  appraisedValue: number
  seniorClaims: { lienType: LienType; creditor: string; amount: number; lienRank: number }[]
}) => api.post<ApplicationDetail>(`/applications/${id}/collaterals`, body)

export const calculateLimit = (id: number, creditScore: number) =>
  api.post<ApplicationDetail>(`/applications/${id}/limits/calculate`, { creditScore })

export const screen = (id: number, body: { incomeVerified: boolean; ownershipVerified: boolean; comment?: string }) =>
  api.post<ApplicationDetail>(`/applications/${id}/screenings`, body)

export const approve = (id: number, body: { approvedAmount?: number; comment?: string }) =>
  api.post<ApplicationDetail>(`/applications/${id}/approvals`, body)

export const confirmPreconditions = (id: number, conditionsFulfilled: boolean) =>
  api.post<ApplicationDetail>(`/applications/${id}/execution-preconditions`, { conditionsFulfilled })

export const execute = (id: number, idempotencyKey: string) =>
  api.post<Execution>(`/applications/${id}/executions`, null, { headers: { 'Idempotency-Key': idempotencyKey } })

export const getPolicies = () => api.get<Policy[]>('/policies')
export const createPolicy = (body: PolicyCreateRequest) => api.post<Policy>('/policies', body)

export const getLoanAccount = (id: number) => api.get<LoanAccountResponse>(`/accounts/${id}`)
export const getSchedule = (id: number) => api.get<Installment[]>(`/accounts/${id}/repayments`)
export const getRepaymentHistory = (id: number) => api.get<RepaymentHistory[]>(`/accounts/${id}/repayment-history`)

export const errorMessage = (e: unknown) =>
  (axios.isAxiosError(e) && e.response?.data?.message) || '요청 처리 중 오류가 발생했습니다.'

// ─── 화면 표시용 ────────────────────────────────────────

export const won = (n: number | null | undefined) => (n == null ? '-' : `${n.toLocaleString()}원`)
export const pct = (n: number | null | undefined) => (n == null ? '-' : `${n}%`)

export const ROLE_LABEL: Record<OperatorRole, string> = {
  OPERATIONS: '여신 실무',
  UNDERWRITER: '심사역',
  APPROVER: '승인권자',
}

/** 담당자 대기열 탭. 단계마다 처리할 역할이 다르다 */
export const QUEUE_TABS: { status?: ApplicationStatus; label: string; role?: OperatorRole }[] = [
  { status: 'APPLIED', label: '담보 평가 대기', role: 'OPERATIONS' },
  { status: 'COLLATERAL_EVALUATED', label: '한도 산정 대기', role: 'UNDERWRITER' },
  { status: 'LIMIT_CALCULATED', label: '심사 대기', role: 'UNDERWRITER' },
  { status: 'DOCUMENT_REQUIRED', label: '추가 서류', role: 'UNDERWRITER' },
  { status: 'SCREENING_APPROVED', label: '승인 대기', role: 'APPROVER' },
  { status: 'CONDITIONALLY_APPROVED', label: '조건부 승인 대기', role: 'APPROVER' },
  { status: 'AGREED', label: '선행조건 확인', role: 'OPERATIONS' },
  { status: 'EXECUTION_READY', label: '실행 대기', role: 'OPERATIONS' },
  { label: '전체' },
]

export const STATUS_COLOR: Record<ApplicationStatus, string> = {
  APPLIED: '#5f6368',
  COLLATERAL_EVALUATED: '#1a73e8',
  LIMIT_CALCULATED: '#1a73e8',
  DOCUMENT_REQUIRED: '#f29900',
  SCREENING_APPROVED: '#188038',
  CONDITIONALLY_APPROVED: '#f29900',
  REJECTED: '#d93025',
  APPROVED: '#188038',
  AGREED: '#188038',
  EXECUTION_READY: '#9334e6',
  EXECUTED: '#202124',
  WITHDRAWN: '#9aa0a6',
}

export const LIEN_LABEL: Record<LienType, string> = {
  SENIOR_MORTGAGE: '선순위 근저당',
  LEASE_DEPOSIT: '임차보증금',
  OWN_MORTGAGE: '당행 근저당',
}

export const CONSTRAINT_LABEL: Record<LimitResult['bindingConstraint'], string> = {
  COLLATERAL: '담보가용액 (LTV − 선순위)',
  DTI: 'DTI',
  STRESS_DSR: '스트레스 DSR',
  PRODUCT: '상품 한도',
  REGULATORY: '규제 최대 금액',
}

export const AREA_COLOR: Record<AreaResult, string> = { PASS: '#188038', HOLD: '#f29900', FAIL: '#d93025' }
