export const PATH = {
  QUEUE: '/',
  DETAIL: '/applications/:applicationId',
  POLICIES: '/policies',
  LOAN_ACCOUNT: '/accounts/:loanAccountId',
} as const

export const detailPath = (id: number) => `/applications/${id}`
export const loanAccountPath = (id: number) => `/accounts/${id}`
