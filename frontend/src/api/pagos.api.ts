import { apiClient } from '@/api/axios';
import type { PagoRequest, PagoResponse } from '@/types/pago';

const PAGOS_PATH = '/pagos';

export async function listarPagosPorAlquiler(alquilerId: number): Promise<PagoResponse[]> {
  const response = await apiClient.get<PagoResponse[]>(`${PAGOS_PATH}/alquiler/${alquilerId}`);
  return response.data;
}

export async function registrarPago(request: PagoRequest): Promise<PagoResponse> {
  const response = await apiClient.post<PagoResponse>(PAGOS_PATH, request);
  return response.data;
}