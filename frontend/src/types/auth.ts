export type UserRole = 'ADMIN' | 'EMPLEADO' | 'DEMO';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: 'Bearer';
  username: string;
  rol: UserRole;
}

export type AuthSession = LoginResponse;
