import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { LoanProduct } from '../api/screening_api'

interface LoanProductState {
  products: LoanProduct[]
  selectedProduct: LoanProduct | null
  setProducts: (products: LoanProduct[]) => void
  setSelectedProduct: (product: LoanProduct) => void
  clearSelectedProduct: () => void
}

export const useLoanProductStore = create<LoanProductState>()(
  persist(
    (set) => ({
      products: [],
      selectedProduct: null,
      setProducts: (products) => set({ products }),
      setSelectedProduct: (product) => set({ selectedProduct: product }),
      clearSelectedProduct: () => set({ selectedProduct: null }),
    }),
    { name: 'loan-products' }
  )
)
