import { api } from './axios'

// 주택담보대출 원장은 Core/mortgage에 있고, bankEx 서버가 고객 요청을 대신 전달한다

export type RepaymentMethod = 'EQUAL_INSTALLMENT' | 'EQUAL_PRINCIPAL' | 'BULLET'

export interface MortgageProduct {
    productCode: string
    productName: string
    minAmount: number
    maxAmount: number
    minTermMonths: number
    maxTermMonths: number
    baseRate: number
    description: string
}

export interface MortgageDebtInput {
    debtType: 'MORTGAGE' | 'CREDIT' | 'JEONSE' | 'OTHER'
    institution: string
    balance: number
    annualPrincipal: number
    annualInterest: number
}

export interface MortgageApplyRequest {
    userId: string
    productCode: string
    requestedAmount: number
    termMonths: number
    repaymentMethod: RepaymentMethod
    loanPurpose: 'PURCHASE' | 'LIVING'
    houseCount: number
    firstHomeBuyer: boolean
    property: {
        address: string
        regionCode: 'REGULATED' | 'NON_REGULATED'
        propertyType: 'APARTMENT' | 'VILLA' | 'DETACHED' | 'OFFICETEL'
        exclusiveArea: number | null
        declaredPrice: number
    }
    income: {
        incomeType: 'EMPLOYEE' | 'BUSINESS' | 'OTHER'
        annualIncome: number
        employerName: string
    }
    debts: MortgageDebtInput[]
}

export interface MortgageApplication {
    applicationId: number
    applicationNo: string
    productCode: string
    requestedAmount: number
    termMonths: number | null
    repaymentMethod: RepaymentMethod | null
    status: string
    statusLabel: string
    notice: string
    appliedAt: string
    propertyAddress: string | null
    approval: {
        approvedAmount: number
        interestRate: number
        termMonths: number
        repaymentMethod: RepaymentMethod
        conditions: string | null
        expiresOn: string
    } | null
    agreement: {
        agreementNo: string
        disbursementAccountNo: string
        agreedAmount: number
        agreedAt: string
    } | null
    loanAccountId: number | null
}

export interface MortgageInstallment {
    installmentNo: number
    dueDate: string
    principal: number
    interest: number
    totalPayment: number
    balanceAfter: number
    status: 'SCHEDULED' | 'PAID'
    paidAt: string | null
}

export interface MortgageLoanAccount {
    account: {
        loanAccountId: number
        loanAccountNo: string
        productCode: string
        principal: number
        outstandingPrincipal: number
        interestRate: number
        termMonths: number
        executionDate: string
        maturityDate: string
        status: string
    }
    statusLabel: string
    repaymentMethodLabel: string
    nextInstallment: MortgageInstallment | null
}

export interface MortgageRepayResponse {
    installmentNo: number
    totalPaid: number
    principalPaid: number
    interestPaid: number
    outstandingAfter: number
    sourceAccountNumber: string
    balanceAfter: number
    transactionNumber: string
}

export const REPAYMENT_METHOD_LABEL: Record<RepaymentMethod, string> = {
    EQUAL_INSTALLMENT: '원리금균등',
    EQUAL_PRINCIPAL: '원금균등',
    BULLET: '만기일시',
}

export async function getMortgageProductsApi(): Promise<MortgageProduct[]> {
    const response = await api.get<MortgageProduct[]>('/loan/mortgage/products')
    return response.data
}

export async function applyMortgageApi(request: MortgageApplyRequest): Promise<MortgageApplication> {
    const response = await api.post<MortgageApplication>('/loan/mortgage/applications', request)
    return response.data
}

export async function getMortgageApplicationsApi(userId: string): Promise<MortgageApplication[]> {
    const response = await api.get<MortgageApplication[]>('/loan/mortgage/applications', { params: { userId } })
    return response.data
}

export async function getMortgageApplicationApi(applicationId: number, userId: string): Promise<MortgageApplication> {
    const response = await api.get<MortgageApplication>(`/loan/mortgage/applications/${applicationId}`, { params: { userId } })
    return response.data
}

export async function agreeMortgageApi(
    applicationId: number,
    userId: string,
    disbursementAccountNumber: string
): Promise<MortgageApplication> {
    const response = await api.post<MortgageApplication>(`/loan/mortgage/applications/${applicationId}/agreement`, {
        userId,
        disbursementAccountNumber,
        consent: true,
    })
    return response.data
}

export async function withdrawMortgageApi(applicationId: number, userId: string): Promise<MortgageApplication> {
    const response = await api.post<MortgageApplication>(`/loan/mortgage/applications/${applicationId}/withdraw`, { userId })
    return response.data
}

export async function getMortgageAccountsApi(userId: string): Promise<MortgageLoanAccount[]> {
    const response = await api.get<MortgageLoanAccount[]>('/loan/mortgage/accounts', { params: { userId } })
    return response.data
}

export async function getMortgageAccountApi(loanAccountId: number, userId: string): Promise<MortgageLoanAccount> {
    const response = await api.get<MortgageLoanAccount>(`/loan/mortgage/accounts/${loanAccountId}`, { params: { userId } })
    return response.data
}

export async function getMortgageScheduleApi(loanAccountId: number, userId: string): Promise<MortgageInstallment[]> {
    const response = await api.get<MortgageInstallment[]>(`/loan/mortgage/accounts/${loanAccountId}/schedule`, { params: { userId } })
    return response.data
}

export async function repayMortgageApi(
    loanAccountId: number,
    userId: string,
    sourceAccountNumber: string
): Promise<MortgageRepayResponse> {
    const response = await api.post<MortgageRepayResponse>(`/loan/mortgage/accounts/${loanAccountId}/repayments`, {
        userId,
        sourceAccountNumber,
    })
    return response.data
}
