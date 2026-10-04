import { create } from 'zustand'

export interface NewLoanCondition {
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

interface RefinanceWizardState {
  customerId: number | null
  selectedLoanIds: number[]
  newLoanCondition: NewLoanCondition | null
  applicationId: number | null
  applicationNo: string | null
  setCustomer: (customerId: number) => void
  setSelectedLoanIds: (loanIds: number[]) => void
  setNewLoanCondition: (condition: NewLoanCondition) => void
  setApplication: (applicationId: number, applicationNo: string) => void
  reset: () => void
}

export const useRefinanceWizardStore = create<RefinanceWizardState>()((set) => ({
  customerId: null,
  selectedLoanIds: [],
  newLoanCondition: null,
  applicationId: null,
  applicationNo: null,
  setCustomer: (customerId) => set({ customerId }),
  setSelectedLoanIds: (loanIds) => set({ selectedLoanIds: loanIds }),
  setNewLoanCondition: (condition) => set({ newLoanCondition: condition }),
  setApplication: (applicationId, applicationNo) => set({ applicationId, applicationNo }),
  reset: () =>
    set({
      customerId: null,
      selectedLoanIds: [],
      newLoanCondition: null,
      applicationId: null,
      applicationNo: null,
    }),
}))
