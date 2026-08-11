import { apiClient } from '@/api/axios';
import type { CategoriaRequest, CategoriaResponse } from '@/types/categoria';

const CATEGORIAS_PATH = '/categorias';

export async function listarCategorias(): Promise<CategoriaResponse[]> {
  const response = await apiClient.get<CategoriaResponse[]>(CATEGORIAS_PATH);
  return response.data;
}

export async function buscarCategoriaPorId(id: number): Promise<CategoriaResponse> {
  const response = await apiClient.get<CategoriaResponse>(`${CATEGORIAS_PATH}/${id}`);
  return response.data;
}

export async function crearCategoria(request: CategoriaRequest): Promise<CategoriaResponse> {
  const response = await apiClient.post<CategoriaResponse>(CATEGORIAS_PATH, request);
  return response.data;
}

export async function actualizarCategoria(id: number, request: CategoriaRequest): Promise<CategoriaResponse> {
  const response = await apiClient.put<CategoriaResponse>(`${CATEGORIAS_PATH}/${id}`, request);
  return response.data;
}

export async function desactivarCategoria(id: number): Promise<void> {
  await apiClient.delete(`${CATEGORIAS_PATH}/${id}`);
}
