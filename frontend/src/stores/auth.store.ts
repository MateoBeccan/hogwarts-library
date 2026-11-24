import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import * as authApi from '@/api/auth.api';
import type { AuthSession, LoginRequest, UserRole } from '@/types/auth';

const STORAGE_KEY = 'hogwarts-library.auth';

function expiresAt(token: string): number {
  try {
    const payload = token.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(payload ?? '')) as { exp?: number };
    return typeof exp === 'number' ? exp * 1000 : 0;
  } catch {
    return 0;
  }
}

function readStoredSession(): AuthSession | null {
  try {
    const session = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? 'null') as AuthSession | null;
    if (session && expiresAt(session.token) > Date.now()) {
      return session;
    }
  } catch {
    // Una sesión dañada se descarta igual que una vencida.
  }
  sessionStorage.removeItem(STORAGE_KEY);
  return null;
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(readStoredSession());

  const token = computed(() => session.value?.token ?? null);
  const username = computed(() => session.value?.username ?? '');
  const role = computed<UserRole | null>(() => session.value?.rol ?? null);
  const isDemo = computed(() => role.value === 'DEMO');

  function hasActiveSession(): boolean {
    return token.value !== null && expiresAt(token.value) > Date.now();
  }

  function startSession(response: AuthSession): void {
    if (!response?.token || expiresAt(response.token) <= Date.now()) {
      throw new Error('Respuesta de autenticación inválida');
    }
    session.value = response;
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    localStorage.removeItem(STORAGE_KEY);
  }

  async function login(credentials: LoginRequest): Promise<void> {
    startSession(await authApi.login(credentials));
  }

  async function loginDemo(): Promise<void> {
    startSession(await authApi.loginDemo());
  }

  function logout(): void {
    session.value = null;
    sessionStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(STORAGE_KEY);
  }

  return {
    token,
    username,
    role,
    isDemo,
    hasActiveSession,
    login,
    loginDemo,
    logout,
  };
});
