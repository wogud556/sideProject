export const PATH = {
  OPERATOR_SELECT: '/',
  DASHBOARD: '/dashboard',
  CUSTOMER_SEARCH: '/customers',
  CUSTOMER_LOANS: '/customers/:customerId/loans',
  REFINANCE_WIZARD: '/refinance/apply',
  REFINANCE_REVIEW: '/refinance/review',
  REFINANCE_EXECUTION: '/refinance/execution',
  FAILURE_RETRY: '/refinance/failures',
  APPLICATION_HISTORY: '/refinance/history',
} as const
