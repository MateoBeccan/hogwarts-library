import { apiClient } from '@/api/axios';
import type { AutorRequest, AutorResponse } from '@/types/autor';

const AUTORES_PATH = '/autores';

export async function listarAutores(): Promise<AutorResponse[]> {
  const response = await apiClient.get<AutorResponse[]>(AUTORES_PATH);
  return response.data;
}

export async function buscarAutorPorId(id: number): Promise<AutorResponse> {
  const response = await apiClient.get<AutorResponse>(`${AUTORES_PATH}/${id}`);
  return response.data;
}

export async function crearAutor(request: AutorRequest): Promise<AutorResponse> {
  const response = await apiClient.post<AutorResponse>(AUTORES_PATH, request);
  return response.data;
}

export async function actualizarAutor(id: number, request: AutorRequest): Promise<AutorResponse> {
  const response = await apiClient.put<AutorResponse>(`${AUTORES_PATH}/${id}`, request);
  return response.data;
}

export async function desactivarAutor(id: number): Promise<void> {
  await apiClient.delete(`${AUTORES_PATH}/${id}`);
}