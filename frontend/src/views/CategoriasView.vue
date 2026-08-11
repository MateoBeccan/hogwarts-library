<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as categoriasApi from '@/api/categorias.api';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { CategoriaResponse } from '@/types/categoria';

const route = useRoute();
const toastStore = useToastStore();
const authStore = useAuthStore();

const categorias = ref<CategoriaResponse[]>([]);
const selectedCategoria = ref<CategoriaResponse | null>(null);
const isLoading = ref(true);
const actionId = ref<number | null>(null);
const errorMessage = ref('');
const successMessage = ref('');

const canDelete = computed(() => authStore.role === 'ADMIN');

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
    successMessage.value = 'Categoria creada correctamente.';
  }

  if (route.query.saved === 'updated') {
    successMessage.value = 'Categoria actualizada correctamente.';
  }
}

async function loadCategorias(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    categorias.value = await categoriasApi.listarCategorias();

    if (selectedCategoria.value) {
      selectedCategoria.value = categorias.value.find((categoria) => categoria.id === selectedCategoria.value?.id) ?? null;
    }
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar las categorias.');
  } finally {
    isLoading.value = false;
  }
}

function selectCategoria(categoria: CategoriaResponse): void {
  selectedCategoria.value = categoria;
}

async function desactivar(categoria: CategoriaResponse): Promise<void> {
  const confirmed = window.confirm(`Desactivar categoria ${categoria.nombre}?`);

  if (!confirmed) {
    return;
  }

  try {
    actionId.value = categoria.id;
    errorMessage.value = '';
    successMessage.value = '';
    await categoriasApi.desactivarCategoria(categoria.id);
    await loadCategorias();
    toastStore.success('Categoria desactivada correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar la categoria.');
  } finally {
    actionId.value = null;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadCategorias();
});
</script>

<template>
  <section>
    <PageHeader title="Categorias" subtitle="Organiza el catalogo de la biblioteca.">
      <RouterLink class="button-link button-link--primary" to="/categorias/nueva">+ Nueva categoria</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="data-card categories-card">
      <div v-if="isLoading" class="table-skeleton table-skeleton--compact" aria-label="Cargando categorias">
        <span v-for="row in 5" :key="row" />
      </div>

      <div v-else-if="!errorMessage && categorias.length === 0" class="empty-state empty-state--compact">
        <strong>No hay categorias</strong>
        <p>Cuando registres categorias, apareceran en este listado.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table app-table--compact">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Descripcion</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="categoria in categorias" :key="categoria.id">
              <td>{{ categoria.nombre }}</td>
              <td>{{ categoria.descripcion || '-' }}</td>
              <td>
                <StatusBadge :tone="categoria.activa ? 'success' : 'danger'">
                  {{ categoria.activa ? 'ACTIVA' : 'INACTIVA' }}
                </StatusBadge>
              </td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="selectCategoria(categoria)">Ver</button>
                  <RouterLink :to="`/categorias/${categoria.id}/editar`">Editar</RouterLink>
                  <button
                    v-if="canDelete && categoria.activa"
                    type="button"
                    :disabled="actionId === categoria.id"
                    @click="desactivar(categoria)"
                  >
                    Desactivar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside v-if="selectedCategoria" class="compact-detail-panel">
      <div>
        <p class="eyebrow">Categoria seleccionada</p>
        <h2>{{ selectedCategoria.nombre }}</h2>
      </div>
      <p>{{ selectedCategoria.descripcion || 'Sin descripcion registrada.' }}</p>
      <StatusBadge :tone="selectedCategoria.activa ? 'success' : 'danger'">
        {{ selectedCategoria.activa ? 'ACTIVA' : 'INACTIVA' }}
      </StatusBadge>
    </aside>
  </section>
</template>
