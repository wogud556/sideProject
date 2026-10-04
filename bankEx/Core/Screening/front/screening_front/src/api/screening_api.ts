import api from './axios'

// ─── 타입 정의 ───────────────────────────────────────────

export interface SignupRequest {
  userId: string
  password: string
  userName: string
  birthDate?: string
  phoneNumber?: string
}

export interface LoginRequest {
  userId: string
  password: string
}

export interface LoginResponse {
  userId: string
  userName: string
  message: string
}

export interface UserProfile {
  userId: string
  userName: string
  phoneNumber: string
  creditScore: number
  annualIncome: number
  employmentType: string
  companyName: string
  employmentMonths: number
  existingDebt: number
  annualRepayment: number
}

export interface LoanProduct {
  productId: number
  productName: string
  productType: string
  minInterestRate: number
  maxInterestRate: number
  maxLimitAmount: number
  loanPeriodMonths: number
}

export interface LoanApplicationRequest {
  userId: string
  productId: number
  requestAmount: number
}

export interface LoanApplicationResponse {
  applicationId: number
  userId: string
  productId: number
  requestAmount: number
  applicationStatus: string
  createdAt: string
}

export interface ScreeningResponse {
  applicationId: number
  cssScore: number
  dsrRate: number
  resultStatus: string
  approvedAmount: number
  interestRate: number
  rejectReason: string | null
}

// ─── API 함수 ─────────────────────────────────────────────

export const signup = (data: SignupRequest) =>
  api.post<string>('/users/signup', data)

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/users/login', data)

export const getProfile = (userId: string) =>
  api.get<UserProfile>(`/users/${userId}/profile`)

export const getProducts = () =>
  api.get<LoanProduct[]>('/products')

export const getProduct = (productId: number) =>
  api.get<LoanProduct>(`/products/${productId}`)

export const applyLoan = (data: LoanApplicationRequest) =>
  api.post<LoanApplicationResponse>('/applications', data)

export const runScreening = (applicationId: number) =>
  api.post<ScreeningResponse>(`/applications/${applicationId}/screening`)

export const getMyApplications = (userId: string) =>
  api.get<LoanApplicationResponse[]>(`/applications/my/${userId}`)

export const getScreeningResult = (applicationId: number) =>
  api.get<ScreeningResponse>(`/applications/${applicationId}/result`)
