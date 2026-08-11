<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as librosApi from '@/api/libros.api';
import AppButton from '@/components/common/AppButton.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';
import type { LibroResponse } from '@/types/libro';

const route = useRoute();
const authStore = useAuthStore();

const libro = ref<LibroResponse | null>(null);
const isLoading = ref(true);
const isActionLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const libroId = computed(() => Number(route.params.id));
const canManageState = computed(() => authStore.role === 'ADMIN');

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) {
      return 'No tienes permisos para realizar esta operacion.';
    }

    if (error.response?.status === 404) {
      return 'No encontramos el libro solicitado.';
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

async function loadLibro(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    libro.value = await librosApi.buscarLibroPorId(libroId.value);
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el libro.');
  } finally {
    isLoading.value = false;
  }
}

async function desactivar(): Promise<void> {
  if (!libro.value) return;

  const confirmed = window.confirm(`Desactivar libro ${libro.value.titulo}?`);

  if (!confirmed) {
    return;
  }

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await librosApi.desactivarLibro(libro.value.id);
    await loadLibro();
    successMessage.value = 'Libro desactivado correctamente.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar el libro.');
  } finally {
    isActionLoading.value = false;
  }
}

async function reactivar(): Promise<void> {
  if (!libro.value) return;

  const confirmed = window.confirm(`Reactivar libro ${libro.value.titulo}?`);

  if (!confirmed) {
    return;
  }

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await librosApi.reactivarLibro(libro.value.id);
    await loadLibro();
    successMessage.value = 'Libro reactivado correctamente.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos reactivar el libro.');
  } finally {
    isActionLoading.value = false;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadLibro();
});
</script>

<template>
  <section>
    <PageHeader title="Detalle de libro" subtitle="Informacion bibliografica de la obra seleccionada.">
      <div class="header-actions">
        <RouterLink class="button-link button-link--ghost" to="/libros">Volver</RouterLink>
        <RouterLink v-if="libro" class="button-link button-link--primary" :to="`/libros/${libro.id}/editar`">Editar</RouterLink>
      </div>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="detail-skeleton" aria-label="Cargando libro">
      <span v-for="row in 6" :key="row" />
    </div>

    <article v-else-if="libro" class="detail-card book-detail-card">
      <div class="detail-card__header">
        <div>
          <p class="eyebrow">Libro</p>
          <h2>{{ libro.titulo }}</h2>
        </div>
        <StatusBadge :tone="libro.activo ? 'success' : 'danger'">
          {{ libro.activo ? 'ACTIVO' : 'INACTIVO' }}
        </StatusBadge>
      </div>

      <dl class="detail-grid">
        <div>
          <dt>Autores</dt>
          <dd>
            <span v-if="libro.autores.length === 0">-</span>
            <span v-else class="author-chips author-chips--inline">
              <span v-for="autor in libro.autores" :key="autor.id" class="author-chip">{{ autor.nombreCompleto }}</span>
            </span>
          </dd>
        </div>
        <div>
          <dt>ISBN</dt>
          <dd>{{ libro.isbn }}</dd>
        </div>
        <div>
          <dt>Editorial</dt>
          <dd>{{ libro.editorial || '-' }}</dd>
        </div>
        <div>
          <dt>Anio</dt>
          <dd>{{ libro.anioPublicacion || '-' }}</dd>
        </div>
        <div>
          <dt>Categoria</dt>
          <dd>{{ libro.categoriaNombre }}</dd>
        </div>
      </dl>

      <section class="description-panel">
        <h3>Descripcion</h3>
        <p>{{ libro.descripcion || 'Sin descripcion registrada.' }}</p>
      </section>

      <div v-if="canManageState" class="detail-actions">
        <AppButton v-if="libro.activo" variant="ghost" :loading="isActionLoading" @click="desactivar">
          Desactivar libro
        </AppButton>
        <AppButton v-else variant="ghost" :loading="isActionLoading" @click="reactivar">
          Reactivar libro
        </AppButton>
      </div>
    </article>

    <div v-else class="empty-state">
      <strong>No encontramos el libro</strong>
      <p>Vuelve al catalogo para seleccionar otra obra.</p>
      <RouterLink class="button-link button-link--primary" to="/libros">Ir a libros</RouterLink>
    </div>
  </section>
</template>
