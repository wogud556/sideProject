import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { LoanApplicationResponse, ScreeningResponse } from '../api/screening_api'

interface LoanApplicationState {
  currentApplication: LoanApplicationResponse | null
  screeningResult: ScreeningResponse | null
  setCurrentApplication: (application: LoanApplicationResponse) => void
  setScreeningResult: (result: ScreeningResponse) => void
  clear: () => void
}

export const useLoanApplicationStore = create<LoanApplicationState>()(
  persist(
    (set) => ({
      currentApplication: null,
      screeningResult: null,
      setCurrentApplication: (application) => set({ currentApplication: application }),
      setScreeningResult: (result) => set({ screeningResult: result }),
      clear: () => set({ currentApplication: null, screeningResult: null }),
    }),
    { name: 'loan-application' }
  )
)
