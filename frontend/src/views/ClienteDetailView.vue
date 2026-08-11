<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as clientesApi from '@/api/clientes.api';
import AppButton from '@/components/common/AppButton.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';
import type { ClienteResponse } from '@/types/cliente';

const route = useRoute();
const authStore = useAuthStore();

const cliente = ref<ClienteResponse | null>(null);
const isLoading = ref(true);
const isActionLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const clienteId = computed(() => Number(route.params.id));
const canManageState = computed(() => authStore.role === 'ADMIN');

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) {
      return 'No tienes permisos para realizar esta operacion.';
    }

    if (error.response?.status === 404) {
      return 'No encontramos el cliente solicitado.';
    }

    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

function applyRouteMessage(): void {
  if (route.query.saved === 'created') {
    successMessage.value = 'Cliente creado correctamente.';
  }

  if (route.query.saved === 'updated') {
    successMessage.value = 'Cliente actualizado correctamente.';
  }
}

async function loadCliente(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    cliente.value = await clientesApi.buscarClientePorId(clienteId.value);
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el cliente.');
  } finally {
    isLoading.value = false;
  }
}

async function desactivar(): Promise<void> {
  if (!cliente.value) return;

  const confirmed = window.confirm(`Desactivar cliente ${cliente.value.nombre} ${cliente.value.apellido}?`);

  if (!confirmed) {
    return;
  }

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await clientesApi.desactivarCliente(cliente.value.id);
    await loadCliente();
    successMessage.value = 'Cliente desactivado correctamente.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar el cliente.');
  } finally {
    isActionLoading.value = false;
  }
}

async function reactivar(): Promise<void> {
  if (!cliente.value) return;

  const confirmed = window.confirm(`Reactivar cliente ${cliente.value.nombre} ${cliente.value.apellido}?`);

  if (!confirmed) {
    return;
  }

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    await clientesApi.reactivarCliente(cliente.value.id);
    await loadCliente();
    successMessage.value = 'Cliente reactivado correctamente.';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos reactivar el cliente.');
  } finally {
    isActionLoading.value = false;
  }
}

onMounted(() => {
  applyRouteMessage();
  void loadCliente();
});
</script>

<template>
  <section>
    <PageHeader title="Detalle de cliente" subtitle="Informacion registrada para la persona seleccionada.">
      <div class="header-actions">
        <RouterLink class="button-link button-link--ghost" to="/clientes">Volver</RouterLink>
        <RouterLink v-if="cliente" class="button-link button-link--primary" :to="`/clientes/${cliente.id}/editar`">Editar</RouterLink>
      </div>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="detail-skeleton" aria-label="Cargando cliente">
      <span v-for="row in 5" :key="row" />
    </div>

    <article v-else-if="cliente" class="detail-card">
      <div class="detail-card__header">
        <div>
          <p class="eyebrow">Cliente</p>
          <h2>{{ cliente.nombre }} {{ cliente.apellido }}</h2>
        </div>
        <StatusBadge :tone="cliente.activo ? 'success' : 'danger'">
          {{ cliente.activo ? 'ACTIVO' : 'INACTIVO' }}
        </StatusBadge>
      </div>

      <dl class="detail-grid">
        <div>
          <dt>DNI</dt>
          <dd>{{ cliente.dni }}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{{ cliente.email }}</dd>
        </div>
        <div>
          <dt>Telefono</dt>
          <dd>{{ cliente.telefono || '-' }}</dd>
        </div>
        <div>
          <dt>Fecha de registro</dt>
          <dd>{{ cliente.fechaRegistro }}</dd>
        </div>
      </dl>

      <div v-if="canManageState" class="detail-actions">
        <AppButton v-if="cliente.activo" variant="ghost" :loading="isActionLoading" @click="desactivar">
          Desactivar cliente
        </AppButton>
        <AppButton v-else variant="ghost" :loading="isActionLoading" @click="reactivar">
          Reactivar cliente
        </AppButton>
      </div>
    </article>

    <div v-else class="empty-state">
      <strong>No encontramos el cliente</strong>
      <p>Vuelve al listado para seleccionar otro registro.</p>
      <RouterLink class="button-link button-link--primary" to="/clientes">Ir a clientes</RouterLink>
    </div>
  </section>
</template>
