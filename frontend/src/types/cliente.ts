export interface ClienteRequest {
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  telefono: string;
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  telefono: string | null;
  fechaRegistro: string;
  activo: boolean;
}
