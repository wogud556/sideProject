import axios from 'axios'
import api from './axios'

// ─── 타입 정의 ───────────────────────────────────────────

export type LoanType = 'GENERAL' | 'JEONSE'
export type ScreeningStatus = 'APPROVED' | 'REJECTED' | 'MANUAL_REVIEW'

export interface ScreeningRequest {
  customerId: string
  loanProductId: string
  loanType: LoanType
  requestedAmount: number
  annualIncome: number
  creditScore: number
  existingLoanAmount: number
}

export interface ScreeningResponse {
  screeningId: number
  customerId: string
  loanProductId: string
  loanType: LoanType
  requestedAmount: number
  annualIncome: number
  creditScore: number
  existingLoanAmount: number
  status: ScreeningStatus
  approvedAmount: number | null
  approvedInterestRate: number | null
  reasonCode: string
  reasonMessage: string
  screenedAt: string
  reviewerId: string | null
  reviewComment: string | null
  reviewedAt: string | null
}

export interface ReviewDecisionRequest {
  reviewerId: string
  approvedAmount?: number
  comment?: string
}

// ─── API 함수 ─────────────────────────────────────────────

export const requestScreening = (data: ScreeningRequest) =>
  api.post<ScreeningResponse>('/loans', data)

export const getScreenings = (status?: ScreeningStatus) =>
  api.get<ScreeningResponse[]>('', { params: { status } })

export const getScreening = (screeningId: number) =>
  api.get<ScreeningResponse>(`/${screeningId}`)

export const approveScreening = (screeningId: number, data: ReviewDecisionRequest) =>
  api.post<ScreeningResponse>(`/${screeningId}/approve`, data)

export const rejectScreening = (screeningId: number, data: ReviewDecisionRequest) =>
  api.post<ScreeningResponse>(`/${screeningId}/reject`, data)

export const errorMessage = (e: unknown) =>
  (axios.isAxiosError(e) && e.response?.data?.message) || '요청 처리 중 오류가 발생했습니다.'

// ─── 화면 표시용 ──────────────────────────────────────────

export const STATUS_LABEL: Record<ScreeningStatus, string> = {
  APPROVED: '승인',
  REJECTED: '거절',
  MANUAL_REVIEW: '수동심사 대기',
}

export const STATUS_COLOR: Record<ScreeningStatus, string> = {
  APPROVED: '#34a853',
  REJECTED: '#ea4335',
  MANUAL_REVIEW: '#f29900',
}

export const LOAN_TYPE_LABEL: Record<LoanType, string> = {
  GENERAL: '일반대출',
  JEONSE: '전세대출',
}
