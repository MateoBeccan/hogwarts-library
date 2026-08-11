<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as ejemplaresApi from '@/api/ejemplares.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { EjemplarResponse, EstadoEjemplar } from '@/types/ejemplar';

const route = useRoute();
const toastStore = useToastStore();

const ejemplares = ref<EjemplarResponse[]>([]);
const search = ref('');
const statusFilter = ref<'TODOS' | EstadoEjemplar>('TODOS');
const isLoading = ref(true);
const actionId = ref<number | null>(null);
const errorMessage = ref('');
const successMessage = ref('');

const filterOptions: Array<{ label: string; value: 'TODOS' | EstadoEjemplar }> = [
  { label: 'Todos', value: 'TODOS' },
  { label: 'Disponibles', value: 'DISPONIBLE' },
  { label: 'Alquilados', value: 'ALQUILADO' },
  { label: 'Fuera de servicio', value: 'FUERA_SERVICIO' },
];

const filteredEjemplares = computed(() => {
  const term = search.value.trim().toLowerCase();

  return ejemplares.value.filter((ejemplar) => {
    const matchesStatus = statusFilter.value === 'TODOS' || ejemplar.estado === statusFilter.value;
    const searchable = `${ejemplar.codigo} ${ejemplar.libroTitulo} ${ejemplar.libroIsbn}`.toLowerCase();
    const matchesSearch = !term || searchable.includes(term);
    return matchesStatus && matchesSearch;
  });
});

function statusTone(estado: EstadoEjemplar): 'success' | 'warning' | 'danger' {
  if (estado === 'DISPONIBLE') return 'success';
  if (estado === 'ALQUILADO') return 'warning';
  return 'danger';
}

function statusLabel(estado: EstadoEjemplar): string {
  if (estado === 'FUERA_SERVICIO') return 'FUERA SERVICIO';
  return estado;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

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
  if (route.query.saved === 'created') successMessage.value = 'Ejemplar creado correctamente.';
  if (route.query.saved === 'updated') successMessage.value = 'Ejemplar actualizado correctamente.';
}

async function loadEjemplares(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    ejemplares.value = await ejemplaresApi.listarEjemplares();
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los ejemplares.');
  } finally {
    isLoading.value = false;
  }
}

async function marcarFueraServicio(ejemplar: EjemplarResponse): Promise<void> {
  const confirmed = window.confirm(`Marcar fuera de servicio el ejemplar ${ejemplar.codigo}?`);

  if (!confirmed) return;

  try {
    actionId.value = ejemplar.id;
    errorMessage.value = '';
    successMessage.value = '';
    await ejemplaresApi.marcarFueraServicio(ejemplar.id);
    await loadEjemplares();
    toastStore.success('Ejemplar marcado fuera de servicio.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cambiar el estado del ejemplar.');
  } finally {
    actionId.value = null;
  }
}

async function habilitar(ejemplar: EjemplarResponse): Promise<void> {
  const confirmed = window.confirm(`Habilitar el ejemplar ${ejemplar.codigo}?`);

  if (!confirmed) return;

  try {
    actionId.value = ejemplar.id;
    errorMessage.value = '';
    successMessage.value = '';
    await ejemplaresApi.habilitarEjemplar(ejemplar.id);
    await loadEjemplares();
    toastStore.success('Ejemplar habilitado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos habilitar el ejemplar.');
  } finally {
    actionId.value = null;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadEjemplares();
});
</script>

<template>
  <section>
    <PageHeader title="Ejemplares" subtitle="Administra las copias fisicas disponibles para alquiler.">
      <RouterLink class="button-link button-link--primary" to="/ejemplares/nuevo">+ Nuevo ejemplar</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="table-toolbar table-toolbar--stacked">
      <AppInput
        id="ejemplar-search"
        v-model="search"
        label="Buscar"
        placeholder="Buscar por codigo, titulo o ISBN..."
      />
      <div class="segmented-control" aria-label="Filtrar por estado">
        <button
          v-for="option in filterOptions"
          :key="option.value"
          type="button"
          :class="{ 'segmented-control__item--active': statusFilter === option.value }"
          @click="statusFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>
      <AppButton v-if="errorMessage" variant="ghost" @click="loadEjemplares">Reintentar</AppButton>
    </section>

    <section class="data-card">
      <div v-if="isLoading" class="table-skeleton" aria-label="Cargando ejemplares">
        <span v-for="row in 6" :key="row" />
      </div>

      <div v-else-if="!errorMessage && filteredEjemplares.length === 0" class="empty-state">
        <strong>No hay ejemplares para mostrar</strong>
        <p>No se encontraron registros con los filtros actuales.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table app-table--ejemplares">
          <thead>
            <tr>
              <th>Codigo</th>
              <th>Libro</th>
              <th>Precio por dia</th>
              <th>Estado</th>
              <th>Fecha alta</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ejemplar in filteredEjemplares" :key="ejemplar.id">
              <td>{{ ejemplar.codigo }}</td>
              <td>
                <strong>{{ ejemplar.libroTitulo }}</strong>
                <span class="table-muted">ISBN {{ ejemplar.libroIsbn }}</span>
              </td>
              <td>{{ formatCurrency(ejemplar.precioDia) }}</td>
              <td>
                <StatusBadge :tone="statusTone(ejemplar.estado)">{{ statusLabel(ejemplar.estado) }}</StatusBadge>
              </td>
              <td>{{ ejemplar.fechaAlta }}</td>
              <td>
                <div class="row-actions">
                  <RouterLink :to="`/ejemplares/${ejemplar.id}`">Ver</RouterLink>
                  <RouterLink v-if="ejemplar.estado !== 'ALQUILADO'" :to="`/ejemplares/${ejemplar.id}/editar`">Editar</RouterLink>
                  <button
                    v-if="ejemplar.estado === 'DISPONIBLE'"
                    type="button"
                    :disabled="actionId === ejemplar.id"
                    @click="marcarFueraServicio(ejemplar)"
                  >
                    Fuera servicio
                  </button>
                  <button
                    v-if="ejemplar.estado === 'FUERA_SERVICIO'"
                    type="button"
                    :disabled="actionId === ejemplar.id"
                    @click="habilitar(ejemplar)"
                  >
                    Habilitar
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
