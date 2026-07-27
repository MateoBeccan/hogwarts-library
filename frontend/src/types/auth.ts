export type UserRole = 'ADMIN' | 'EMPLEADO';

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

export interface AuthSession {
  token: string;
  type: 'Bearer';
  username: string;
  rol: UserRole;
}
