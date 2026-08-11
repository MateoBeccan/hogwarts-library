<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import * as alquileresApi from '@/api/alquileres.api';
import * as clientesApi from '@/api/clientes.api';
import * as ejemplaresApi from '@/api/ejemplares.api';
import * as librosApi from '@/api/libros.api';
import alquileresIcon from '@/assets/images/hogwarts-library-alquileres-logo.png';
import clientesIcon from '@/assets/images/hogwarts-library-clientes-logo.png';
import ejemplaresIcon from '@/assets/images/hogwarts-library-ejemplares-logo.png';
import librosIcon from '@/assets/images/hogwarts-library-libros-logo.png';
import AppButton from '@/components/common/AppButton.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatCard from '@/components/common/StatCard.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { AlquilerResponse, EstadoAlquiler } from '@/types/alquiler';
import type { ClienteResponse } from '@/types/cliente';
import type { EjemplarResponse } from '@/types/ejemplar';
import type { LibroResponse } from '@/types/libro';

const clientes = ref<ClienteResponse[]>([]);
const libros = ref<LibroResponse[]>([]);
const ejemplares = ref<EjemplarResponse[]>([]);
const alquileres = ref<AlquilerResponse[]>([]);
const isLoading = ref(true);
const errors = ref<Record<string, string>>({});

const metricCards = computed(() => [
  {
    icon: 'C',
    iconImage: clientesIcon,
    label: 'Clientes activos',
    value: String(clientes.value.filter((cliente) => cliente.activo).length),
    description: errors.value.clientes ?? 'Clientes habilitados para operar',
    failed: Boolean(errors.value.clientes),
  },
  {
    icon: 'L',
    iconImage: librosIcon,
    label: 'Libros activos',
    value: String(libros.value.filter((libro) => libro.activo).length),
    description: errors.value.libros ?? 'Titulos activos del catalogo',
    failed: Boolean(errors.value.libros),
  },
  {
    icon: 'E',
    iconImage: ejemplaresIcon,
    label: 'Ejemplares disponibles',
    value: String(ejemplares.value.filter((ejemplar) => ejemplar.estado === 'DISPONIBLE').length),
    description: errors.value.ejemplares ?? 'Copias listas para alquilar',
    failed: Boolean(errors.value.ejemplares),
  },
  {
    icon: 'A',
    iconImage: alquileresIcon,
    label: 'Alquileres activos',
    value: String(alquileres.value.filter((alquiler) => alquiler.estado === 'ACTIVO').length),
    description: errors.value.alquileres ?? 'Alquileres actualmente abiertos',
    failed: Boolean(errors.value.alquileres),
  },
]);

const rentalStatusSummary = computed(() => {
  const summary: Record<EstadoAlquiler, number> = {
    ACTIVO: 0,
    FINALIZADO: 0,
    CANCELADO: 0,
  };

  alquileres.value.forEach((alquiler) => {
    summary[alquiler.estado] += 1;
  });

  return [
    { label: 'Activos', status: 'ACTIVO' as const, value: summary.ACTIVO, tone: 'warning' as const },
    { label: 'Finalizados', status: 'FINALIZADO' as const, value: summary.FINALIZADO, tone: 'success' as const },
    { label: 'Cancelados', status: 'CANCELADO' as const, value: summary.CANCELADO, tone: 'danger' as const },
  ];
});

const totalAlquileres = computed(() => alquileres.value.length);
const recentAlquileres = computed(() =>
  [...alquileres.value]
    .sort((a, b) => b.fechaAlquiler.localeCompare(a.fechaAlquiler) || b.id - a.id)
    .slice(0, 5),
);

const quickActions = [
  { label: '+ Nuevo cliente', path: '/clientes/nuevo' },
  { label: '+ Nuevo libro', path: '/libros/nuevo' },
  { label: '+ Nuevo ejemplar', path: '/ejemplares/nuevo' },
  { label: '+ Nuevo alquiler', path: '/alquileres/nuevo' },
];

function statusTone(estado: EstadoAlquiler): 'success' | 'warning' | 'danger' {
  if (estado === 'ACTIVO') return 'warning';
  if (estado === 'FINALIZADO') return 'success';
  return 'danger';
}

function statusWidth(value: number): string {
  if (totalAlquileres.value === 0) return '0%';
  return `${Math.max((value / totalAlquileres.value) * 100, value > 0 ? 8 : 0)}%`;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) return 'Sin permisos para cargar estos datos.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

async function loadDashboard(): Promise<void> {
  isLoading.value = true;
  errors.value = {};

  const [clientesResult, librosResult, ejemplaresResult, alquileresResult] = await Promise.allSettled([
    clientesApi.listarClientes(),
    librosApi.listarLibros(),
    ejemplaresApi.listarEjemplares(),
    alquileresApi.listarAlquileres(),
  ]);

  if (clientesResult.status === 'fulfilled') clientes.value = clientesResult.value;
  else errors.value.clientes = resolveError(clientesResult.reason, 'No pudimos cargar clientes.');

  if (librosResult.status === 'fulfilled') libros.value = librosResult.value;
  else errors.value.libros = resolveError(librosResult.reason, 'No pudimos cargar libros.');

  if (ejemplaresResult.status === 'fulfilled') ejemplares.value = ejemplaresResult.value;
  else errors.value.ejemplares = resolveError(ejemplaresResult.reason, 'No pudimos cargar ejemplares.');

  if (alquileresResult.status === 'fulfilled') alquileres.value = alquileresResult.value;
  else errors.value.alquileres = resolveError(alquileresResult.reason, 'No pudimos cargar alquileres.');

  isLoading.value = false;
}

onMounted(() => {
  void loadDashboard();
});
</script>

<template>
  <section class="dashboard">
    <PageHeader title="Dashboard" subtitle="Resumen general de la biblioteca" />

    <div v-if="isLoading" class="stats-grid" aria-label="Cargando metricas">
      <div v-for="item in 4" :key="item" class="stat-card stat-card--loading">
        <span />
        <span />
        <span />
      </div>
    </div>

    <div v-else class="stats-grid">
      <StatCard
        v-for="metric in metricCards"
        :key="metric.label"
        :class="{ 'stat-card--error': metric.failed }"
        :icon="metric.icon"
        :icon-image="metric.iconImage"
        :label="metric.label"
        :value="metric.failed ? 'Error' : metric.value"
        :description="metric.description"
      />
    </div>

    <section class="workspace-section dashboard-rental-status">
      <div class="section-heading section-heading--with-action section-heading--plain">
        <div>
          <h2>Estado de alquileres</h2>
          <p>Distribucion actual calculada desde los alquileres cargados.</p>
        </div>
        <AppButton v-if="errors.alquileres" variant="ghost" @click="loadDashboard">Reintentar</AppButton>
      </div>

      <div v-if="errors.alquileres" class="alert alert--danger dashboard-alert" role="alert">
        {{ errors.alquileres }}
      </div>

      <div v-else-if="totalAlquileres === 0" class="empty-state empty-state--compact">
        <strong>No hay alquileres registrados todavia.</strong>
      </div>

      <div v-else class="rental-status-list">
        <article v-for="item in rentalStatusSummary" :key="item.status" class="rental-status-item">
          <div>
            <StatusBadge :tone="item.tone">{{ item.status }}</StatusBadge>
            <strong>{{ item.value }}</strong>
          </div>
          <div class="rental-status-bar" aria-hidden="true">
            <span :class="`rental-status-bar__fill rental-status-bar__fill--${item.status.toLowerCase()}`" :style="{ width: statusWidth(item.value) }" />
          </div>
        </article>
      </div>
    </section>

    <section class="data-card dashboard-recents">
      <div class="section-heading">
        <h2>Alquileres recientes</h2>
      </div>

      <div v-if="errors.alquileres" class="alert alert--danger dashboard-alert" role="alert">
        {{ errors.alquileres }}
      </div>

      <div v-else-if="recentAlquileres.length === 0" class="empty-state empty-state--compact">
        <strong>No hay alquileres registrados todavia.</strong>
      </div>

      <div v-else class="table-wrap">
        <table class="app-table app-table--dashboard-rentals">
          <thead>
            <tr>
              <th>Cliente</th>
              <th>Fecha</th>
              <th>Vencimiento</th>
              <th>Total</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="alquiler in recentAlquileres" :key="alquiler.id">
              <td>
                <RouterLink :to="`/alquileres/${alquiler.id}`">{{ alquiler.clienteNombreCompleto }}</RouterLink>
              </td>
              <td>{{ alquiler.fechaAlquiler }}</td>
              <td>{{ alquiler.fechaVencimiento }}</td>
              <td>{{ formatCurrency(alquiler.total) }}</td>
              <td><StatusBadge :tone="statusTone(alquiler.estado)">{{ alquiler.estado }}</StatusBadge></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="quick-actions">
      <h2>Acciones rapidas</h2>
      <div class="quick-actions__grid">
        <RouterLink v-for="action in quickActions" :key="action.path" :to="action.path">{{ action.label }}</RouterLink>
      </div>
    </section>
  </section>
</template>
