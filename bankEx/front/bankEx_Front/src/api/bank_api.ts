import { api } from './axios'

export interface UserResponse {
    userId: string,
    userName: string,
    balance: number
}

// Post /user
export async function postUserApi(): Promise<UserResponse> {
    const response = await api.post<UserResponse>('/user')
    return response.data
}