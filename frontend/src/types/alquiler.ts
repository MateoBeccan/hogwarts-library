export type EstadoAlquiler = 'ACTIVO' | 'FINALIZADO' | 'CANCELADO';

export interface AlquilerRequest {
  clienteId: number | null;
  fechaVencimiento: string;
  ejemplarIds: number[];
  observaciones: string;
}

export interface DetalleAlquilerResponse {
  id: number;
  ejemplarId: number;
  ejemplarCodigo: string;
  libroId: number;
  libroTitulo: string;
  precioDia: number;
  cantidadDias: number;
  subtotal: number;
}

export interface AlquilerResponse {
  id: number;
  clienteId: number;
  clienteNombreCompleto: string;
  fechaAlquiler: string;
  fechaVencimiento: string;
  fechaDevolucion: string | null;
  estado: EstadoAlquiler;
  total: number;
  observaciones: string | null;
  detalles: DetalleAlquilerResponse[];
}