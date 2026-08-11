export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';

export interface PagoRequest {
  alquilerId: number;
  monto: string;
  metodoPago: MetodoPago | '';
  observaciones: string;
}

export interface PagoResponse {
  id: number;
  alquilerId: number;
  fecha: string;
  monto: number;
  metodoPago: MetodoPago;
  observaciones: string | null;
  totalAlquiler: number;
  totalPagado: number;
  saldoPendiente: number;
}