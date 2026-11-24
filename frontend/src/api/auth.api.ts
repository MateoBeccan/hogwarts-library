import { apiClient } from '@/api/axios';
import type { LoginRequest, LoginResponse } from '@/types/auth';

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/auth/login', request);
  return response.data;
}

export async function loginDemo(): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/auth/demo', null, { timeout: 90_000 });
  return response.data;
}
