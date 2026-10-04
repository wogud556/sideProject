import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { CustomerResponse } from '../api/refinance_api'

interface CustomerSearchState {
  selectedCustomer: CustomerResponse | null
  setSelectedCustomer: (customer: CustomerResponse) => void
  clearSelectedCustomer: () => void
}

export const useCustomerSearchStore = create<CustomerSearchState>()(
  persist(
    (set) => ({
      selectedCustomer: null,
      setSelectedCustomer: (customer) => set({ selectedCustomer: customer }),
      clearSelectedCustomer: () => set({ selectedCustomer: null }),
    }),
    { name: 'customer-search' }
  )
)
