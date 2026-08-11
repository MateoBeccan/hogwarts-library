import { apiClient } from '@/api/axios';
import type { ClienteRequest, ClienteResponse } from '@/types/cliente';

const CLIENTES_PATH = '/clientes';

export async function listarClientes(): Promise<ClienteResponse[]> {
  const response = await apiClient.get<ClienteResponse[]>(CLIENTES_PATH);
  return response.data;
}

export async function buscarClientePorId(id: number): Promise<ClienteResponse> {
  const response = await apiClient.get<ClienteResponse>(`${CLIENTES_PATH}/${id}`);
  return response.data;
}

export async function crearCliente(request: ClienteRequest): Promise<ClienteResponse> {
  const response = await apiClient.post<ClienteResponse>(CLIENTES_PATH, request);
  return response.data;
}

export async function actualizarCliente(id: number, request: ClienteRequest): Promise<ClienteResponse> {
  const response = await apiClient.put<ClienteResponse>(`${CLIENTES_PATH}/${id}`, request);
  return response.data;
}

export async function desactivarCliente(id: number): Promise<void> {
  await apiClient.delete(`${CLIENTES_PATH}/${id}`);
}

export async function reactivarCliente(id: number): Promise<ClienteResponse> {
  const response = await apiClient.patch<ClienteResponse>(`${CLIENTES_PATH}/${id}/reactivar`);
  return response.data;
}
