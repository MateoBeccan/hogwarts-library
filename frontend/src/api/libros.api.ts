import { apiClient } from '@/api/axios';
import type { LibroRequest, LibroResponse } from '@/types/libro';

const LIBROS_PATH = '/libros';

export async function listarLibros(): Promise<LibroResponse[]> {
  const response = await apiClient.get<LibroResponse[]>(LIBROS_PATH);
  return response.data;
}

export async function buscarLibroPorId(id: number): Promise<LibroResponse> {
  const response = await apiClient.get<LibroResponse>(`${LIBROS_PATH}/${id}`);
  return response.data;
}

export async function crearLibro(request: LibroRequest): Promise<LibroResponse> {
  const response = await apiClient.post<LibroResponse>(LIBROS_PATH, request);
  return response.data;
}

export async function actualizarLibro(id: number, request: LibroRequest): Promise<LibroResponse> {
  const response = await apiClient.put<LibroResponse>(`${LIBROS_PATH}/${id}`, request);
  return response.data;
}

export async function desactivarLibro(id: number): Promise<void> {
  await apiClient.delete(`${LIBROS_PATH}/${id}`);
}

export async function reactivarLibro(id: number): Promise<LibroResponse> {
  const response = await apiClient.patch<LibroResponse>(`${LIBROS_PATH}/${id}/reactivar`);
  return response.data;
}
