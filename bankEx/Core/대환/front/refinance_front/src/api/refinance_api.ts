import api from './axios'

// ─── 타입 정의: 고객/기존대출 ───

export interface CustomerResponse {
  customerId: number
  customerNo: string
  maskedName: string
  maskedPhone: string
  maskedBirthDate: string
  status: string
}

export interface ExistingLoanResponse {
  loanId: number
  financialInstitutionName: string
  maskedLoanAccountNo: string
  loanProductName: string
  loanType: string
  originalAmount: number
  currentBalance: number
  interestRate: number
  executionDate: string
  maturityDate: string
  repaymentMethod: string
  overdue: boolean
  refinanceEligible: boolean
}

// ─── API 함수: 고객/기존대출 ───

export async function searchCustomersApi(params: {
  customerNo?: string
  name?: string
  birthDate?: string
  phone?: string
}): Promise<CustomerResponse[]> {
  const res = await api.get<CustomerResponse[]>('/customers', { params })
  return res.data
}

export async function getCustomerApi(customerId: number): Promise<CustomerResponse> {
  const res = await api.get<CustomerResponse>(`/customers/${customerId}`)
  return res.data
}

export async function getCustomerLoansApi(customerId: number): Promise<ExistingLoanResponse[]> {
  const res = await api.get<ExistingLoanResponse[]>(`/customers/${customerId}/loans`)
  return res.data
}

export async function getLoanApi(loanId: number): Promise<ExistingLoanResponse> {
  const res = await api.get<ExistingLoanResponse>(`/loans/${loanId}`)
  return res.data
}

// ─── 타입 정의: 대환 가능여부 / 상환금액 ───

export interface EligibilityResult {
  loanId: number | null
  code: string
  passed: boolean
  message: string
}

export interface EligibilityResponse {
  eligible: boolean
  results: EligibilityResult[]
}

export interface RepaymentAmountResponse {
  loanId: number
  principalBalance: number
  accruedInterest: number
  prepaymentFee: number
  otherCost: number
  discountAmount: number
  finalRepaymentAmount: number
  calculatedAt: string
}

// ─── API 함수: 대환 가능여부 / 상환금액 ───

export async function checkEligibilityApi(customerId: number, loanIds: number[]): Promise<EligibilityResponse> {
  const res = await api.post<EligibilityResponse>('/refinance/eligibility', { customerId, loanIds })
  return res.data
}

export async function repaymentInquiryApi(loanIds: number[]): Promise<RepaymentAmountResponse[]> {
  const res = await api.post<RepaymentAmountResponse[]>('/refinance/repayment-inquiry', { loanIds })
  return res.data
}

// ─── 타입 정의: 대환 신청 ───

export interface RefinanceTargetResponse {
  targetId: number
  loanId: number
  financialInstitutionCode: string
  maskedLoanAccountNo: string
  loanProductCode: string
  loanBalance: number
  repaymentAmount: number
  prepaymentFee: number
  interestAmount: number
}

export type RefinanceStatus =
  | 'DRAFT' | 'REQUESTED' | 'REVIEWING' | 'APPROVED' | 'REJECTED'
  | 'EXECUTING' | 'NEW_LOAN_EXECUTED' | 'REPAYING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export interface RefinanceApplicationResponse {
  applicationId: number
  applicationNo: string
  customerId: number
  status: RefinanceStatus
  applicationDate: string
  requestedAmount: number
  approvedAmount: number | null
  newLoanProductName: string
  newLoanAmount: number
  newLoanRate: number
  newLoanRateType: string
  newLoanPeriodMonths: number
  newLoanMaturityDate: string
  newLoanRepaymentMethod: string
  newLoanExecutionScheduledDate: string
  newLoanAccountNo: string
  refinancePurposeYn: string
  rejectReasonCode: string | null
  targets: RefinanceTargetResponse[]
}

export interface RefinanceApplyRequest {
  customerId: number
  loanIds: number[]
  newLoanProductName: string
  newLoanAmount: number
  newLoanRate: number
  newLoanRateType: string
  newLoanPeriodMonths: number
  newLoanRepaymentMethod: string
  newLoanExecutionScheduledDate: string
  newLoanAccountNo: string
  refinancePurposeYn: 'Y' | 'N'
}

// ─── API 함수: 대환 신청 ───

export async function applyRefinanceApi(request: RefinanceApplyRequest): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>('/refinance/applications', request)
  return res.data
}

export async function listRefinanceApplicationsApi(): Promise<RefinanceApplicationResponse[]> {
  const res = await api.get<RefinanceApplicationResponse[]>('/refinance/applications')
  return res.data
}

export async function getRefinanceApplicationApi(applicationId: number): Promise<RefinanceApplicationResponse> {
  const res = await api.get<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}`)
  return res.data
}

// ─── API 함수: 심사/승인/거절 ───

export async function reviewApplicationApi(applicationId: number, opinion: string): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}/review`, { opinion })
  return res.data
}

export async function approveApplicationApi(
  applicationId: number,
  body: { approvedAmount?: number; approvalCondition?: string; approvalMemo?: string }
): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}/approve`, body)
  return res.data
}

export async function rejectApplicationApi(applicationId: number, rejectReason: string): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}/reject`, { rejectReason })
  return res.data
}

// ─── API 함수: 실행 ───

export async function executeApplicationApi(applicationId: number): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}/execute`)
  return res.data
}

// ─── 타입/API 함수: 실패거래 / 재처리 ───

export interface ErrorSearchResult {
  errorId: number
  applicationId: number
  applicationNo: string
  customerId: number
  failedStep: string
  errorCode: string
  errorMessage: string
  status: string
  retryCount: number
  createdAt: string
}

export async function searchErrorsApi(params: {
  transactionDate?: string
  applicationNo?: string
  customerId?: number
  failedStep?: string
  errorCode?: string
  status?: string
}): Promise<ErrorSearchResult[]> {
  const res = await api.get<ErrorSearchResult[]>('/refinance/errors', { params })
  return res.data
}

export async function retryApplicationApi(applicationId: number): Promise<RefinanceApplicationResponse> {
  const res = await api.post<RefinanceApplicationResponse>(`/refinance/applications/${applicationId}/retry`)
  return res.data
}

// ─── 타입/API 함수: 업무이력 ───

export interface RefinanceHistoryResponse {
  historyId: number
  actionType: string
  fromStatus: RefinanceStatus | null
  toStatus: RefinanceStatus
  description: string
  processedBy: string
  processedAt: string
}

export async function getApplicationHistoryApi(applicationId: number): Promise<RefinanceHistoryResponse[]> {
  const res = await api.get<RefinanceHistoryResponse[]>(`/refinance/applications/${applicationId}/history`)
  return res.data
}

// ─── 타입/API 함수: Dashboard ───

export interface DashboardResponse {
  todayApplicationCount: number
  reviewingCount: number
  approvedCount: number
  executionPendingCount: number
  completedCount: number
  failedCount: number
}

export async function getDashboardApi(): Promise<DashboardResponse> {
  const res = await api.get<DashboardResponse>('/refinance/dashboard')
  return res.data
}
