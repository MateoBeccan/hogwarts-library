<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import * as clientesApi from '@/api/clientes.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { ClienteResponse } from '@/types/cliente';

const authStore = useAuthStore();
const toastStore = useToastStore();

const clientes = ref<ClienteResponse[]>([]);
const search = ref('');
const isLoading = ref(true);
const actionId = ref<number | null>(null);
const errorMessage = ref('');
const successMessage = ref('');

const canManageState = computed(() => authStore.role === 'ADMIN');

const filteredClientes = computed(() => {
  const term = search.value.trim().toLowerCase();

  if (!term) {
    return clientes.value;
  }

  return clientes.value.filter((cliente) => {
    const searchable = `${cliente.nombre} ${cliente.apellido} ${cliente.dni}`.toLowerCase();
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

async function loadClientes(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    clientes.value = await clientesApi.listarClientes();
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los clientes.');
  } finally {
    isLoading.value = false;
  }
}

async function desactivar(cliente: ClienteResponse): Promise<void> {
  const confirmed = window.confirm(`Desactivar cliente ${cliente.nombre} ${cliente.apellido}?`);

  if (!confirmed) {
    return;
  }

  try {
    actionId.value = cliente.id;
    errorMessage.value = '';
    successMessage.value = '';
    await clientesApi.desactivarCliente(cliente.id);
    await loadClientes();
    toastStore.success('Cliente desactivado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos desactivar el cliente.');
  } finally {
    actionId.value = null;
  }
}

async function reactivar(cliente: ClienteResponse): Promise<void> {
  const confirmed = window.confirm(`Reactivar cliente ${cliente.nombre} ${cliente.apellido}?`);

  if (!confirmed) {
    return;
  }

  try {
    actionId.value = cliente.id;
    errorMessage.value = '';
    successMessage.value = '';
    await clientesApi.reactivarCliente(cliente.id);
    await loadClientes();
    toastStore.success('Cliente reactivado correctamente.');
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos reactivar el cliente.');
  } finally {
    actionId.value = null;
  }
}

onMounted(() => {
  void loadClientes();
});
</script>

<template>
  <section>
    <PageHeader title="Clientes" subtitle="Administra las personas registradas en la biblioteca.">
      <RouterLink class="button-link button-link--primary" to="/clientes/nuevo">+ Nuevo cliente</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <section class="table-toolbar">
      <AppInput
        id="cliente-search"
        v-model="search"
        label="Buscar"
        placeholder="Buscar por nombre, apellido o DNI..."
      />
      <AppButton v-if="errorMessage" variant="ghost" @click="loadClientes">Reintentar</AppButton>
    </section>

    <section class="data-card">
      <div v-if="isLoading" class="table-skeleton" aria-label="Cargando clientes">
        <span v-for="row in 6" :key="row" />
      </div>

      <div v-else-if="!errorMessage && filteredClientes.length === 0" class="empty-state">
        <strong>No hay clientes para mostrar</strong>
        <p v-if="search">No se encontraron coincidencias con la busqueda actual.</p>
        <p v-else>Cuando registres clientes, apareceran en este listado.</p>
      </div>

      <div v-else-if="!errorMessage" class="table-wrap">
        <table class="app-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Apellido</th>
              <th>DNI</th>
              <th>Email</th>
              <th>Telefono</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cliente in filteredClientes" :key="cliente.id">
              <td>{{ cliente.nombre }}</td>
              <td>{{ cliente.apellido }}</td>
              <td>{{ cliente.dni }}</td>
              <td>{{ cliente.email }}</td>
              <td>{{ cliente.telefono || '-' }}</td>
              <td>
                <StatusBadge :tone="cliente.activo ? 'success' : 'danger'">
                  {{ cliente.activo ? 'ACTIVO' : 'INACTIVO' }}
                </StatusBadge>
              </td>
              <td>
                <div class="row-actions">
                  <RouterLink :to="`/clientes/${cliente.id}`">Ver</RouterLink>
                  <RouterLink :to="`/clientes/${cliente.id}/editar`">Editar</RouterLink>
                  <button
                    v-if="canManageState && cliente.activo"
                    type="button"
                    :disabled="actionId === cliente.id"
                    @click="desactivar(cliente)"
                  >
                    Desactivar
                  </button>
                  <button
                    v-if="canManageState && !cliente.activo"
                    type="button"
                    :disabled="actionId === cliente.id"
                    @click="reactivar(cliente)"
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
