<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as ejemplaresApi from '@/api/ejemplares.api';
import AppButton from '@/components/common/AppButton.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { EjemplarResponse, EstadoEjemplar } from '@/types/ejemplar';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const route = useRoute();

const ejemplar = ref<EjemplarResponse | null>(null);
const isLoading = ref(true);
const isActionLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const ejemplarId = computed(() => Number(route.params.id));

function statusTone(estado: EstadoEjemplar): 'success' | 'warning' | 'danger' {
  if (estado === 'DISPONIBLE') return 'success';
  if (estado === 'ALQUILADO') return 'warning';
  return 'danger';
}

function statusLabel(estado: EstadoEjemplar): string {
  return estado === 'FUERA_SERVICIO' ? 'FUERA SERVICIO' : estado;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) return 'No tienes permisos para realizar esta operacion.';
    if (error.response?.status === 404) return 'No encontramos el ejemplar solicitado.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

function applyRouteMessage(): void {
  if (route.query.saved === 'created') successMessage.value = 'Ejemplar creado correctamente.';
  if (route.query.saved === 'updated') successMessage.value = 'Ejemplar actualizado correctamente.';
}

async function loadEjemplar(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    ejemplar.value = await ejemplaresApi.buscarEjemplarPorId(ejemplarId.value);
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el ejemplar.');
  } finally {
    isLoading.value = false;
  }
}

async function marcarFueraServicio(): Promise<void> {
  if (!ejemplar.value) return;

  const confirmed = window.confirm(`Marcar fuera de servicio el ejemplar ${ejemplar.value.codigo}?`);
  if (!confirmed) return;

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await ejemplaresApi.marcarFueraServicio(ejemplar.value.id);
    await loadEjemplar();
    successMessage.value = 'Ejemplar marcado fuera de servicio.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cambiar el estado del ejemplar.');
  } finally {
    isActionLoading.value = false;
  }
}

async function habilitar(): Promise<void> {
  if (!ejemplar.value) return;

  const confirmed = window.confirm(`Habilitar el ejemplar ${ejemplar.value.codigo}?`);
  if (!confirmed) return;

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await ejemplaresApi.habilitarEjemplar(ejemplar.value.id);
    await loadEjemplar();
    successMessage.value = 'Ejemplar habilitado correctamente.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos habilitar el ejemplar.');
  } finally {
    isActionLoading.value = false;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadEjemplar();
});
</script>

<template>
  <section>
    <PageHeader title="Detalle de ejemplar" subtitle="Informacion de la copia fisica seleccionada.">
      <div class="header-actions">
        <RouterLink class="button-link button-link--ghost" to="/ejemplares">Volver</RouterLink>
        <RouterLink
          v-if="!authStore.isDemo && ejemplar && ejemplar.estado !== 'ALQUILADO'"
          class="button-link button-link--primary"
          :to="`/ejemplares/${ejemplar.id}/editar`"
        >
          Editar
        </RouterLink>
      </div>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="detail-skeleton" aria-label="Cargando ejemplar">
      <span v-for="row in 5" :key="row" />
    </div>

    <article v-else-if="ejemplar" class="detail-card">
      <div class="detail-card__header">
        <div>
          <p class="eyebrow">Ejemplar</p>
          <h2>{{ ejemplar.codigo }}</h2>
        </div>
        <StatusBadge :tone="statusTone(ejemplar.estado)">{{ statusLabel(ejemplar.estado) }}</StatusBadge>
      </div>

      <dl class="detail-grid">
        <div>
          <dt>Libro</dt>
          <dd>{{ ejemplar.libroTitulo }}</dd>
        </div>
        <div>
          <dt>ISBN</dt>
          <dd>{{ ejemplar.libroIsbn }}</dd>
        </div>
        <div>
          <dt>Precio por dia</dt>
          <dd>{{ formatCurrency(ejemplar.precioDia) }}</dd>
        </div>
        <div>
          <dt>Fecha de alta</dt>
          <dd>{{ ejemplar.fechaAlta }}</dd>
        </div>
      </dl>

      <section class="description-panel">
        <h3>Observaciones</h3>
        <p>{{ ejemplar.observaciones || 'Sin observaciones registradas.' }}</p>
      </section>

      <div v-if="!authStore.isDemo" class="detail-actions">
        <AppButton
          v-if="ejemplar.estado === 'DISPONIBLE'"
          variant="ghost"
          :loading="isActionLoading"
          @click="marcarFueraServicio"
        >
          Marcar fuera de servicio
        </AppButton>
        <AppButton
          v-if="ejemplar.estado === 'FUERA_SERVICIO'"
          variant="ghost"
          :loading="isActionLoading"
          @click="habilitar"
        >
          Habilitar ejemplar
        </AppButton>
      </div>
    </article>

    <div v-else class="empty-state">
      <strong>No encontramos el ejemplar</strong>
      <p>Vuelve al listado para seleccionar otro registro.</p>
      <RouterLink class="button-link button-link--primary" to="/ejemplares">Ir a ejemplares</RouterLink>
    </div>
  </section>
</template>
