import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type OperatorRole = 'ROLE_TELLER' | 'ROLE_REVIEWER' | 'ROLE_APPROVER' | 'ROLE_OPERATOR' | 'ROLE_ADMIN'

interface OperatorState {
  operatorId: string | null
  name: string | null
  role: OperatorRole | null
  setOperator: (operatorId: string, name: string, role: OperatorRole) => void
  clearOperator: () => void
}

export const useOperatorStore = create<OperatorState>()(
  persist(
    (set) => ({
      operatorId: null,
      name: null,
      role: null,
      setOperator: (operatorId, name, role) => set({ operatorId, name, role }),
      clearOperator: () => set({ operatorId: null, name: null, role: null }),
    }),
    { name: 'operator' }
  )
)
