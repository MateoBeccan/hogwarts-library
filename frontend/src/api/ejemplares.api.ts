import { apiClient } from '@/api/axios';
import type { EjemplarRequest, EjemplarResponse } from '@/types/ejemplar';

const EJEMPLARES_PATH = '/ejemplares';

export async function listarEjemplares(): Promise<EjemplarResponse[]> {
  const response = await apiClient.get<EjemplarResponse[]>(EJEMPLARES_PATH);
  return response.data;
}

export async function buscarEjemplarPorId(id: number): Promise<EjemplarResponse> {
  const response = await apiClient.get<EjemplarResponse>(`${EJEMPLARES_PATH}/${id}`);
  return response.data;
}

export async function crearEjemplar(request: EjemplarRequest): Promise<EjemplarResponse> {
  const response = await apiClient.post<EjemplarResponse>(EJEMPLARES_PATH, request);
  return response.data;
}

export async function actualizarEjemplar(id: number, request: EjemplarRequest): Promise<EjemplarResponse> {
  const response = await apiClient.put<EjemplarResponse>(`${EJEMPLARES_PATH}/${id}`, request);
  return response.data;
}

export async function marcarFueraServicio(id: number): Promise<EjemplarResponse> {
  const response = await apiClient.patch<EjemplarResponse>(`${EJEMPLARES_PATH}/${id}/fuera-servicio`);
  return response.data;
}

export async function habilitarEjemplar(id: number): Promise<EjemplarResponse> {
  const response = await apiClient.patch<EjemplarResponse>(`${EJEMPLARES_PATH}/${id}/habilitar`);
  return response.data;
}
