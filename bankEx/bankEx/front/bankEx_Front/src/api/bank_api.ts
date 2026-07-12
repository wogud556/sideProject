import { api } from './axios'

export interface LoginResponse {
    userId: string
    userName: string
    message: string
}

export interface SignupResponse {
    userId: string
    userName: string
    accountNumber: string
    message: string
}

export interface AccountResponse {
    accountId: number
    accountNumber: string
    balance: number
    createdAt: string
    accountStatus: string
}

export interface AccountCloseResponse {
    accountId: number
    accountNumber: string
    accountStatus: string
    closedAt: string
    message: string
}

export interface LoanProductResponse {
    productId: number
    productName: string
    interestRate: number
    maxLimit: number
    description: string
}

export interface UserProfileResponse {
    userId: string
    userName: string
    phone: string
    createdAt: string
    accounts: AccountResponse[]
}

export interface LoanApplicationResponse {
    applicationId: number
    productName: string
    requestAmount: number
    loanPeriod: number
    status: string
    remainingBalance: number | null
    createdAt: string
}

export interface TransactionResponse {
    accountNumber: string
    transactionType: string
    amount: number
    balanceAfter: number
    description: string
    transactionAt: string
}

export interface LoanDisbursementResponse {
    applicationId: number
    userId: string
    accountNumber: string
    loanAmount: number
    message: string
}

export interface LoanRepaymentResponse {
    applicationId: number
    userId: string
    accountNumber: string
    repaymentAmount: number
    remainingBalance: number
    message: string
}

export interface JeonseLoanProductResponse {
    productId: string
    productName: string
    productType: string
    maxLimitAmount: number
    baseRate: number
    minRate: number
    maxRate: number
}

export interface JeonseContractRequest {
    lessorName: string
    lessorPhone: string
    houseAddress: string
    houseType: string
    capitalAreaYn: string
    depositAmount: number
    downPaymentAmount: number
    contractStartDate: string
    contractEndDate: string
    fixedDateYn: string
    moveInPlanYn: string
    seniorClaimAmount: number
}

export interface JeonseLoanApplyRequest {
    userId: string
    productId: string
    requestAmount: number
    annualIncome: number
    existingDebtAmount: number
    creditScore: number
    homelessYn: string
    householderYn: string
    guaranteeOrg: string
    repaymentType: string
    salaryTransferYn: boolean
    cardUsageYn: boolean
    autoTransferYn: boolean
    contract: JeonseContractRequest
}

export interface JeonseLoanApplyResponse {
    applicationId: string
    status: string
    availableLimitAmount: number | null
    requestAmount: number
    estimatedRate: number | null
    message: string
}

export interface JeonseLoanReviewResponse {
    applicationId: string
    status: string
    approvedAmount: number | null
    loanRate: number | null
    message: string
}

export interface JeonseLoanExecuteResponse {
    applicationId: string
    status: string
    executedAmount: number
    firstPaymentDate: string
    message: string
}

export function extractErrorMessage(err: any, fallback: string): string {
    const data = err?.response?.data
    if (typeof data === 'string') return data
    if (data?.message) return data.message
    return fallback
}

export async function loginApi(userId: string, password: string): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/login', { userId, password })
    return response.data
}

export async function signupApi(
    userId: string,
    password: string,
    userName: string,
    phone: string
): Promise<SignupResponse> {
    const response = await api.post<SignupResponse>('/signup', { userId, password, userName, phone })
    return response.data
}

export async function getAccountsApi(userId: string): Promise<AccountResponse[]> {
    const response = await api.get<AccountResponse[]>('/accounts', { params: { userId } })
    return response.data
}

export async function getLoanProductsApi(): Promise<LoanProductResponse[]> {
    const response = await api.get<LoanProductResponse[]>('/loan/products')
    return response.data
}

export async function getLoanProductApi(productId: number): Promise<LoanProductResponse> {
    const response = await api.get<LoanProductResponse>(`/loan/products/${productId}`)
    return response.data
}

export async function applyLoanApi(
    userId: string,
    accountNumber: string,
    productId: number,
    requestAmount: number,
    loanPeriod: number
): Promise<LoanApplicationResponse> {
    const response = await api.post<LoanApplicationResponse>('/loan/apply', {
        userId,
        accountNumber,
        productId,
        requestAmount,
        loanPeriod,
    })
    return response.data
}

export async function getMyApplicationsApi(userId: string): Promise<LoanApplicationResponse[]> {
    const response = await api.get<LoanApplicationResponse[]>('/loan/applications', { params: { userId } })
    return response.data
}

export async function getUserProfileApi(userId: string): Promise<UserProfileResponse> {
    const response = await api.get<UserProfileResponse>('/profile', { params: { userId } })
    return response.data
}

export async function getAccountApi(accountNumber: string): Promise<AccountResponse> {
    const response = await api.get<AccountResponse>(`/accounts/${accountNumber}`)
    return response.data
}

export async function openAccountApi(
    userId: string,
    productCode: string,
    accountName: string
): Promise<AccountResponse> {
    const response = await api.post<AccountResponse>('/accounts', { userId, productCode, accountName })
    return response.data
}

export async function depositApi(
    accountNumber: string,
    amount: number,
    description: string
): Promise<TransactionResponse> {
    const response = await api.post<TransactionResponse>(`/accounts/${accountNumber}/deposit`, {
        amount,
        description,
    })
    return response.data
}

export async function withdrawApi(
    accountNumber: string,
    amount: number,
    description: string
): Promise<TransactionResponse> {
    const response = await api.post<TransactionResponse>(`/accounts/${accountNumber}/withdraw`, {
        amount,
        description,
    })
    return response.data
}

export async function getTransactionsApi(accountNumber: string): Promise<TransactionResponse[]> {
    const response = await api.get<TransactionResponse[]>(`/accounts/${accountNumber}/transactions`)
    return response.data
}

export async function closeAccountApi(accountNumber: string, userId: string): Promise<AccountCloseResponse> {
    const response = await api.post<AccountCloseResponse>(`/accounts/${accountNumber}/close`, { userId })
    return response.data
}

export async function approveLoanApi(applicationId: number): Promise<LoanApplicationResponse> {
    const response = await api.post<LoanApplicationResponse>(`/loan/applications/${applicationId}/approve`)
    return response.data
}

export async function disburseLoanApi(applicationId: number): Promise<LoanDisbursementResponse> {
    const response = await api.post<LoanDisbursementResponse>('/loan/disbursement', { applicationId })
    return response.data
}

export async function repayLoanApi(
    applicationId: number,
    repaymentAmount: number
): Promise<LoanRepaymentResponse> {
    const response = await api.post<LoanRepaymentResponse>('/loan/repayment', { applicationId, repaymentAmount })
    return response.data
}

export async function getJeonseLoanProductsApi(): Promise<JeonseLoanProductResponse[]> {
    const response = await api.get<JeonseLoanProductResponse[]>('/loan/jeonse/products')
    return response.data
}

export async function applyJeonseLoanApi(request: JeonseLoanApplyRequest): Promise<JeonseLoanApplyResponse> {
    const response = await api.post<JeonseLoanApplyResponse>('/loan/jeonse/applications', request)
    return response.data
}

export async function reviewJeonseLoanApi(applicationId: string): Promise<JeonseLoanReviewResponse> {
    const response = await api.post<JeonseLoanReviewResponse>(`/loan/jeonse/applications/${applicationId}/review`)
    return response.data
}

export async function executeJeonseLoanApi(applicationId: string): Promise<JeonseLoanExecuteResponse> {
    const response = await api.post<JeonseLoanExecuteResponse>(`/loan/jeonse/applications/${applicationId}/execute`)
    return response.data
}
