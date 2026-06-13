import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { LoanApplicationResponse } from '../api/screening_api'

interface MyApplicationState {
  applications: LoanApplicationResponse[]
  setApplications: (applications: LoanApplicationResponse[]) => void
  clear: () => void
}

export const useMyApplicationStore = create<MyApplicationState>()(
  persist(
    (set) => ({
      applications: [],
      setApplications: (applications) => set({ applications }),
      clear: () => set({ applications: [] }),
    }),
    { name: 'my-applications' }
  )
)
