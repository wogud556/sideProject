import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface OperatorState {
  operatorId: string
  setOperatorId: (operatorId: string) => void
}

export const useOperatorStore = create<OperatorState>()(
  persist(
    (set) => ({
      operatorId: 'OPS01',
      setOperatorId: (operatorId) => set({ operatorId }),
    }),
    { name: 'mortgage-operator' }
  )
)
