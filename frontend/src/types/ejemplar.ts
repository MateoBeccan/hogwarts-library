export type EstadoEjemplar = 'DISPONIBLE' | 'ALQUILADO' | 'FUERA_SERVICIO';

export interface EjemplarRequest {
  precioDia: number | null;
  observaciones: string;
  libroId: number | null;
}

export interface EjemplarResponse {
  id: number;
  codigo: string;
  estado: EstadoEjemplar;
  precioDia: number;
  fechaAlta: string;
  observaciones: string | null;
  libroId: number;
  libroTitulo: string;
  libroIsbn: string;
}
