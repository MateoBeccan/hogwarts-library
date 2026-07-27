import axios, { AxiosError } from 'axios';

import router from '@/router';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore();

  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore();
      authStore.logout();

      if (router.currentRoute.value.name !== 'login') {
        void router.push({ name: 'login' });
      }
    }

    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    return error.response?.data?.message ?? 'Ha ocurrido un error inesperado.';
  }

  return 'Ha ocurrido un error inesperado.';
}
