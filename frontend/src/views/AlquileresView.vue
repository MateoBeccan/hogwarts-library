<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import * as alquileresApi from '@/api/alquileres.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { AlquilerResponse, EstadoAlquiler } from '@/types/alquiler';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const alquileres = ref<AlquilerResponse[]>([]);
const search = ref('');
const statusFilter = ref<'TODOS' | EstadoAlquiler>('TODOS');
const isLoading = ref(true);
const errorMessage = ref('');

const filterOptions: Array<{ label: string; value: 'TODOS' | EstadoAlquiler }> = [
  { label: 'Todos', value: 'TODOS' },
  { label: 'Activos', value: 'ACTIVO' },
  { label: 'Finalizados', value: 'FINALIZADO' },
  { label: 'Cancelados', value: 'CANCELADO' },
];

const filteredAlquileres = computed(() => {
  const term = search.value.trim().toLowerCase();

  return alquileres.value.filter((alquiler) => {
    const matchesStatus = statusFilter.value === 'TODOS' || alquiler.estado === statusFilter.value;
    const searchable = `${alquiler.id} ${alquiler.clienteNombreCompleto}`.toLowerCase();
    return matchesStatus && (!term || searchable.includes(term));
  });
});

function statusTone(estado: EstadoAlquiler): 'success' | 'warning' | 'danger' {
  if (estado === 'ACTIVO') return 'warning';
  if (estado === 'FINALIZADO') return 'success';
  return 'danger';
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) return 'No tienes permisos para realizar esta operacion.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

async function loadAlquileres(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    alquileres.value = await alquileresApi.listarAlquileres();
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los alquileres.');
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  void loadAlquileres();
});
</script>

<template>
  <section>
    <PageHeader title="Alquileres" subtitle="Consulta el estado de los alquileres registrados.">
      <RouterLink class="button-link button-link--primary" v-if="!authStore.isDemo" to="/alquileres/nuevo">+ Nuevo alquiler</RouterLink>
    </PageHeader>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="table-toolbar table-toolbar--stacked">
      <AppInput id="alquiler-search" v-model="search" label="Buscar" placeholder="Buscar por ID o cliente..." />
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
      <AppButton v-if="errorMessage" variant="ghost" @click="loadAlquileres">Reintentar</AppButton>
    </section>

    <section class="data-card">
      <div v-if="isLoading" class="table-skeleton" aria-label="Cargando alquileres">
        <span v-for="row in 6" :key="row" />
      </div>

      <div v-else-if="!errorMessage && filteredAlquileres.length === 0" class="empty-state">
        <strong>No hay alquileres para mostrar</strong>
        <p>No se encontraron registros con los filtros actuales.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table app-table--alquileres">
          <thead>
            <tr>
              <th>ID</th>
              <th>Cliente</th>
              <th>Fecha alquiler</th>
              <th>Vencimiento</th>
              <th>Devolucion</th>
              <th>Ejemplares</th>
              <th>Total</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="alquiler in filteredAlquileres" :key="alquiler.id">
              <td>#{{ alquiler.id }}</td>
              <td>{{ alquiler.clienteNombreCompleto }}</td>
              <td>{{ alquiler.fechaAlquiler }}</td>
              <td>{{ alquiler.fechaVencimiento }}</td>
              <td>{{ alquiler.fechaDevolucion || '-' }}</td>
              <td>{{ alquiler.detalles.length }}</td>
              <td>{{ formatCurrency(alquiler.total) }}</td>
              <td><StatusBadge :tone="statusTone(alquiler.estado)">{{ alquiler.estado }}</StatusBadge></td>
              <td>
                <div class="row-actions">
                  <RouterLink :to="`/alquileres/${alquiler.id}`">Ver</RouterLink>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>
