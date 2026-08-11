import type { AutorResponse } from '@/types/autor';

export interface LibroRequest {
  isbn: string;
  titulo: string;
  autorIds: number[];
  editorial: string;
  anioPublicacion: number | null;
  descripcion: string;
  categoriaId: number | null;
}

export interface LibroResponse {
  id: number;
  isbn: string;
  titulo: string;
  autores: AutorResponse[];
  editorial: string | null;
  anioPublicacion: number | null;
  descripcion: string | null;
  activo: boolean;
  categoriaId: number;
  categoriaNombre: string;
}

export function formatAutores(autores: AutorResponse[]): string {
  if (autores.length === 0) {
    return '-';
  }

  return autores.map((autor) => autor.nombreCompleto).join(', ');
}