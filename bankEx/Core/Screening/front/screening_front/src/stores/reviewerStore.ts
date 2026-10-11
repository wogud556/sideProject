import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// 인증은 없다. 결정 기록에 남길 심사역 ID만 보관한다
interface ReviewerState {
  reviewerId: string
  setReviewerId: (reviewerId: string) => void
}

export const useReviewerStore = create<ReviewerState>()(
  persist(
    (set) => ({
      reviewerId: 'REVIEWER01',
      setReviewerId: (reviewerId) => set({ reviewerId }),
    }),
    { name: 'reviewer' }
  )
)
