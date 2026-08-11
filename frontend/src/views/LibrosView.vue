<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as librosApi from '@/api/libros.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { LibroResponse } from '@/types/libro';
import { formatAutores } from '@/types/libro';

const route = useRoute();
const toastStore = useToastStore();
const authStore = useAuthStore();

const libros = ref<LibroResponse[]>([]);
const search = ref('');
const isLoading = ref(true);
const actionId = ref<number | null>(null);
const errorMessage = ref('');
const successMessage = ref('');

const canManageState = computed(() => authStore.role === 'ADMIN');

const filteredLibros = computed(() => {
  const term = search.value.trim().toLowerCase();

  if (!term) {
    return libros.value;
  }

  return libros.value.filter((libro) => {
    const autores = libro.autores.map((autor) => autor.nombreCompleto).join(' ');
    const searchable = `${libro.titulo} ${autores} ${libro.isbn}`.toLowerCase();
    return searchable.includes(term);
  });
});

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) {
      return 'No tienes permisos para realizar esta operacion.';
    }

    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

function applyRouteMessage(): void {
  if (route.query.saved === 'created') {
    successMessage.value = 'Libro creado correctamente.';
  }

  if (route.query.saved === 'updated') {
    successMessage.value = 'Libro actualizado correctamente.';
  }
}

async function loadLibros(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    libros.value = await librosApi.listarLibros();
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los libros.');
  } finally {
    isLoading.value = false;
  }
}

async function desactivar(libro: LibroResponse): Promise<void> {
  const confirmed = window.confirm(`Desactivar libro ${libro.titulo}?`);

  if (!confirmed) {
    return;
  }

  try {
    actionId.value = libro.id;
    errorMessage.value = '';
    successMessage.value = '';
    await librosApi.desactivarLibro(libro.id);
    await loadLibros();
    toastStore.success('Libro desactivado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar el libro.');
  } finally {
    actionId.value = null;
  }
}

async function reactivar(libro: LibroResponse): Promise<void> {
  const confirmed = window.confirm(`Reactivar libro ${libro.titulo}?`);

  if (!confirmed) {
    return;
  }

  try {
    actionId.value = libro.id;
    errorMessage.value = '';
    successMessage.value = '';
    await librosApi.reactivarLibro(libro.id);
    await loadLibros();
    toastStore.success('Libro reactivado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos reactivar el libro.');
  } finally {
    actionId.value = null;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadLibros();
});
</script>

<template>
  <section>
    <PageHeader title="Catalogo de libros" subtitle="Gestiona las obras disponibles en la biblioteca.">
      <RouterLink class="button-link button-link--primary" to="/libros/nuevo">+ Nuevo libro</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="table-toolbar">
      <AppInput
        id="libro-search"
        v-model="search"
        label="Buscar"
        placeholder="Buscar por titulo, autor o ISBN..."
      />
      <AppButton v-if="errorMessage" variant="ghost" @click="loadLibros">Reintentar</AppButton>
    </section>

    <section class="data-card">
      <div v-if="isLoading" class="table-skeleton" aria-label="Cargando libros">
        <span v-for="row in 6" :key="row" />
      </div>

      <div v-else-if="!errorMessage && filteredLibros.length === 0" class="empty-state">
        <strong>No hay libros para mostrar</strong>
        <p v-if="search">No se encontraron coincidencias con la busqueda actual.</p>
        <p v-else>Cuando registres libros, apareceran en este catalogo.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table app-table--books">
          <thead>
            <tr>
              <th>Titulo</th>
              <th>Autor</th>
              <th>ISBN</th>
              <th>Categoria</th>
              <th>Editorial</th>
              <th>Anio</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="libro in filteredLibros" :key="libro.id">
              <td>{{ libro.titulo }}</td>
              <td>{{ formatAutores(libro.autores) }}</td>
              <td>{{ libro.isbn }}</td>
              <td>{{ libro.categoriaNombre }}</td>
              <td>{{ libro.editorial || '-' }}</td>
              <td>{{ libro.anioPublicacion || '-' }}</td>
              <td>
                <StatusBadge :tone="libro.activo ? 'success' : 'danger'">
                  {{ libro.activo ? 'ACTIVO' : 'INACTIVO' }}
                </StatusBadge>
              </td>
              <td>
                <div class="row-actions">
                  <RouterLink :to="`/libros/${libro.id}`">Ver</RouterLink>
                  <RouterLink :to="`/libros/${libro.id}/editar`">Editar</RouterLink>
                  <button
                    v-if="canManageState && libro.activo"
                    type="button"
                    :disabled="actionId === libro.id"
                    @click="desactivar(libro)"
                  >
                    Desactivar
                  </button>
                  <button
                    v-if="canManageState && !libro.activo"
                    type="button"
                    :disabled="actionId === libro.id"
                    @click="reactivar(libro)"
                  >
                    Reactivar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>
