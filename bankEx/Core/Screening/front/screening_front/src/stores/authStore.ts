import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  userId: string | null
  userName: string | null
  isLoggedIn: boolean
  setAuth: (userId: string, userName: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      userId: null,
      userName: null,
      isLoggedIn: false,
      setAuth: (userId, userName) => set({ userId, userName, isLoggedIn: true }),
      clearAuth: () => set({ userId: null, userName: null, isLoggedIn: false }),
    }),
    { name: 'auth' }
  )
)
