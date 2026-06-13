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
    createdAt: string
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
