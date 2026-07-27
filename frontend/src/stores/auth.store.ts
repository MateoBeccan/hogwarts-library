import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import * as authApi from '@/api/auth.api';
import type { AuthSession, LoginRequest, UserRole } from '@/types/auth';

const STORAGE_KEY = 'hogwarts-library.auth';

function readStoredSession(): AuthSession | null {
  const rawSession = localStorage.getItem(STORAGE_KEY);

  if (!rawSession) {
    return null;
  }

  try {
    return JSON.parse(rawSession) as AuthSession;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(readStoredSession());

  const token = computed(() => session.value?.token ?? null);
  const username = computed(() => session.value?.username ?? '');
  const role = computed<UserRole | null>(() => session.value?.rol ?? null);
  const isAuthenticated = computed(() => Boolean(session.value?.token));
  const initials = computed(() => username.value.slice(0, 2).toUpperCase() || 'HL');

  async function login(credentials: LoginRequest): Promise<void> {
    const response = await authApi.login(credentials);

    session.value = {
      token: response.token,
      type: response.type,
      username: response.username,
      rol: response.rol,
    };

    localStorage.setItem(STORAGE_KEY, JSON.stringify(session.value));
  }

  function logout(): void {
    session.value = null;
    localStorage.removeItem(STORAGE_KEY);
  }

  return {
    session,
    token,
    username,
    role,
    isAuthenticated,
    initials,
    login,
    logout,
  };
});
