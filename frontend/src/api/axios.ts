import axios, { AxiosError } from 'axios';

import router from '@/router';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL?.trim().replace(/\/$/, '') || '/api',
  timeout: 15_000,
});

apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore();

  if (!config.url?.startsWith('/auth/') && authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const authStore = useAuthStore();
    const rejectedToken = error.config?.headers.get('Authorization');
    if (error.response?.status === 401 && authStore.token
      && rejectedToken === `Bearer ${authStore.token}`) {
      authStore.logout();
      const current = router.currentRoute.value;
      if (current.name !== 'login') {
        void router.push({ name: 'login', query: { redirect: current.fullPath, session: 'expired' } });
      }
    }

    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) return 'Tu sesión no tiene permiso para realizar esta acción.';
    if (error.response?.status === 429) return 'Hay demasiadas solicitudes. Esperá un momento y reintentá.';
    if (!error.response || error.response.status >= 500 || typeof error.response.data === 'string') {
      return 'No pudimos conectar con la biblioteca. Esperá un momento y reintentá.';
    }
    return error.response.data?.message ?? 'Ha ocurrido un error inesperado.';
  }

  return 'No pudimos completar la solicitud. Esperá un momento y reintentá.';
}
