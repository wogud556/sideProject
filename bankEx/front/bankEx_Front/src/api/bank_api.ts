import { api } from './axios'

export interface UserResponse {
    userId: string,
    userName: string,
    balance: number
}

export interface LoginResponse {
    userId: string,
    userName: string,
    message: string
}

// Post /user
export async function postUserApi(): Promise<UserResponse> {
    const response = await api.post<UserResponse>('/user')
    return response.data
}

// Post /login
export async function loginApi(userId: string, password: string): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/login', { userId, password })
    return response.data
}