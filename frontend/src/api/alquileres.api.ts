import { apiClient } from '@/api/axios';
import type { AlquilerRequest, AlquilerResponse } from '@/types/alquiler';

const ALQUILERES_PATH = '/alquileres';

export async function listarAlquileres(): Promise<AlquilerResponse[]> {
  const response = await apiClient.get<AlquilerResponse[]>(ALQUILERES_PATH);
  return response.data;
}

export async function buscarAlquilerPorId(id: number): Promise<AlquilerResponse> {
  const response = await apiClient.get<AlquilerResponse>(`${ALQUILERES_PATH}/${id}`);
  return response.data;
}

export async function crearAlquiler(request: AlquilerRequest): Promise<AlquilerResponse> {
  const response = await apiClient.post<AlquilerResponse>(ALQUILERES_PATH, request);
  return response.data;
}

export async function finalizarAlquiler(id: number): Promise<AlquilerResponse> {
  const response = await apiClient.patch<AlquilerResponse>(`${ALQUILERES_PATH}/${id}/finalizar`);
  return response.data;
}

export async function cancelarAlquiler(id: number): Promise<AlquilerResponse> {
  const response = await apiClient.patch<AlquilerResponse>(`${ALQUILERES_PATH}/${id}/cancelar`);
  return response.data;
}