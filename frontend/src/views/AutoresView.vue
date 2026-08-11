<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as autoresApi from '@/api/autores.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { AutorResponse } from '@/types/autor';

const route = useRoute();
const authStore = useAuthStore();
const toastStore = useToastStore();

const autores = ref<AutorResponse[]>([]);
const search = ref('');
const isLoading = ref(true);
const actionId = ref<number | null>(null);
const errorMessage = ref('');
const successMessage = ref('');
const pendingAutor = ref<AutorResponse | null>(null);

const canDelete = computed(() => authStore.role === 'ADMIN');

const filteredAutores = computed(() => {
  const term = search.value.trim().toLowerCase();

  if (!term) {
    return autores.value;
  }

  return autores.value.filter((autor) => autor.nombreCompleto.toLowerCase().includes(term));
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
    successMessage.value = 'Autor creado correctamente.';
  }

  if (route.query.saved === 'updated') {
    successMessage.value = 'Autor actualizado correctamente.';
  }
}

async function loadAutores(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    autores.value = await autoresApi.listarAutores();
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los autores.');
  } finally {
    isLoading.value = false;
  }
}

function requestDesactivar(autor: AutorResponse): void {
  pendingAutor.value = autor;
}

function cancelDesactivar(): void {
  if (actionId.value === null) {
    pendingAutor.value = null;
  }
}

async function confirmDesactivar(): Promise<void> {
  if (!pendingAutor.value || actionId.value !== null) {
    return;
  }

  try {
    actionId.value = pendingAutor.value.id;
    errorMessage.value = '';
    successMessage.value = '';
    await autoresApi.desactivarAutor(pendingAutor.value.id);
    pendingAutor.value = null;
    await loadAutores();
    toastStore.success('Autor desactivado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar el autor.');
  } finally {
    actionId.value = null;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadAutores();
});
</script>

<template>
  <section>
    <PageHeader title="Autores" subtitle="Administra las firmas asociadas al catalogo.">
      <RouterLink class="button-link button-link--primary" to="/autores/nuevo">+ Nuevo autor</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="table-toolbar">
      <AppInput id="autor-search" v-model="search" label="Buscar" placeholder="Buscar por nombre completo..." />
      <AppButton v-if="errorMessage" variant="ghost" @click="loadAutores">Reintentar</AppButton>
    </section>

    <section class="data-card authors-card">
      <div v-if="isLoading" class="table-skeleton table-skeleton--compact" aria-label="Cargando autores">
        <span v-for="row in 5" :key="row" />
      </div>

      <div v-else-if="!errorMessage && filteredAutores.length === 0" class="empty-state empty-state--compact">
        <strong>No hay autores para mostrar</strong>
        <p v-if="search">No se encontraron coincidencias con la busqueda actual.</p>
        <p v-else>Cuando registres autores, apareceran en este listado.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table app-table--compact">
          <thead>
            <tr>
              <th>Nombre completo</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="autor in filteredAutores" :key="autor.id">
              <td>{{ autor.nombreCompleto }}</td>
              <td>
                <StatusBadge :tone="autor.activo ? 'success' : 'danger'">
                  {{ autor.activo ? 'ACTIVO' : 'INACTIVO' }}
                </StatusBadge>
              </td>
              <td>
                <div class="row-actions">
                  <RouterLink :to="`/autores/${autor.id}`">Ver</RouterLink>
                  <RouterLink :to="`/autores/${autor.id}/editar`">Editar</RouterLink>
                  <button
                    v-if="canDelete && autor.activo"
                    type="button"
                    :disabled="actionId === autor.id"
                    @click="requestDesactivar(autor)"
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

    <ConfirmDialog
      :open="Boolean(pendingAutor)"
      title="Desactivar autor"
      :message="`El autor ${pendingAutor?.nombreCompleto ?? ''} dejara de estar disponible para nuevas asignaciones.`"
      confirm-label="Desactivar autor"
      :loading="actionId !== null"
      destructive
      @cancel="cancelDesactivar"
      @confirm="confirmDesactivar"
    />
  </section>
</template>