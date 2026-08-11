<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import * as clientesApi from '@/api/clientes.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { ApiError } from '@/types/api';
import type { ClienteRequest } from '@/types/cliente';

const route = useRoute();
const router = useRouter();

const form = reactive<ClienteRequest>({
  nombre: '',
  apellido: '',
  dni: '',
  email: '',
  telefono: '',
});

const fieldErrors = reactive<Record<keyof ClienteRequest, string>>({
  nombre: '',
  apellido: '',
  dni: '',
  email: '',
  telefono: '',
});

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref('');

const clienteId = computed(() => {
  const rawId = route.params.id;
  return typeof rawId === 'string' ? Number(rawId) : null;
});
const isEditing = computed(() => Number.isFinite(clienteId.value));
const pageTitle = computed(() => (isEditing.value ? 'Editar cliente' : 'Nuevo cliente'));

function resetFieldErrors(): void {
  fieldErrors.nombre = '';
  fieldErrors.apellido = '';
  fieldErrors.dni = '';
  fieldErrors.email = '';
  fieldErrors.telefono = '';
}

function validate(): boolean {
  resetFieldErrors();

  if (!form.nombre.trim()) fieldErrors.nombre = 'El nombre es obligatorio';
  if (form.nombre.length > 100) fieldErrors.nombre = 'El nombre no puede superar los 100 caracteres';

  if (!form.apellido.trim()) fieldErrors.apellido = 'El apellido es obligatorio';
  if (form.apellido.length > 100) fieldErrors.apellido = 'El apellido no puede superar los 100 caracteres';

  if (!form.dni.trim()) fieldErrors.dni = 'El DNI es obligatorio';
  if (form.dni.length > 20) fieldErrors.dni = 'El DNI no puede superar los 20 caracteres';

  if (!form.email.trim()) fieldErrors.email = 'El email es obligatorio';
  if (form.email.length > 150) fieldErrors.email = 'El email no puede superar los 150 caracteres';
  if (form.email.trim() && !/^\S+@\S+\.\S+$/.test(form.email)) fieldErrors.email = 'El formato del email no es valido';

  if (form.telefono.length > 30) fieldErrors.telefono = 'El telefono no puede superar los 30 caracteres';

  return !Object.values(fieldErrors).some(Boolean);
}

function applyValidationErrors(errors: Record<string, string>): void {
  Object.entries(errors).forEach(([field, message]) => {
    if (field in fieldErrors) {
      fieldErrors[field as keyof ClienteRequest] = message;
    }
  });
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.data?.validationErrors) {
      applyValidationErrors(error.response.data.validationErrors);
    }

    if (error.response?.status === 403) {
      return 'No tienes permisos para realizar esta operacion.';
    }

    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

async function loadCliente(): Promise<void> {
  if (!isEditing.value || clienteId.value === null) {
    return;
  }

  try {
    isLoading.value = true;
    errorMessage.value = '';
    const cliente = await clientesApi.buscarClientePorId(clienteId.value);
    form.nombre = cliente.nombre;
    form.apellido = cliente.apellido;
    form.dni = cliente.dni;
    form.email = cliente.email;
    form.telefono = cliente.telefono ?? '';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el cliente.');
  } finally {
    isLoading.value = false;
  }
}

async function submit(): Promise<void> {
  if (isSubmitting.value || !validate()) {
    return;
  }

  const request: ClienteRequest = {
    nombre: form.nombre.trim(),
    apellido: form.apellido.trim(),
    dni: form.dni.trim(),
    email: form.email.trim(),
    telefono: form.telefono.trim(),
  };

  try {
    isSubmitting.value = true;
    errorMessage.value = '';
    resetFieldErrors();

    const saved = isEditing.value && clienteId.value !== null
      ? await clientesApi.actualizarCliente(clienteId.value, request)
      : await clientesApi.crearCliente(request);

    await router.push({
      path: `/clientes/${saved.id}`,
      query: { saved: isEditing.value ? 'updated' : 'created' },
    });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos guardar el cliente.');
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(() => {
  void loadCliente();
});
</script>

<template>
  <section>
    <PageHeader
      :title="pageTitle"
      subtitle="Completa los datos personales y de contacto del cliente."
    />

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton" aria-label="Cargando cliente">
      <span v-for="row in 5" :key="row" />
    </div>

    <form v-else class="form-card" @submit.prevent="submit">
      <section class="form-section">
        <h2>Datos personales</h2>
        <div class="form-grid">
          <AppInput id="nombre" v-model="form.nombre" label="Nombre" :error="fieldErrors.nombre" />
          <AppInput id="apellido" v-model="form.apellido" label="Apellido" :error="fieldErrors.apellido" />
          <AppInput id="dni" v-model="form.dni" label="DNI" :error="fieldErrors.dni" />
        </div>
      </section>

      <section class="form-section">
        <h2>Contacto</h2>
        <div class="form-grid">
          <AppInput id="email" v-model="form.email" label="Email" type="email" :error="fieldErrors.email" />
          <AppInput id="telefono" v-model="form.telefono" label="Telefono" :error="fieldErrors.telefono" />
        </div>
      </section>

      <div class="form-actions">
        <RouterLink class="button-link button-link--ghost" to="/clientes">Cancelar</RouterLink>
        <AppButton type="submit" :loading="isSubmitting" :disabled="isSubmitting">
          Guardar cliente
        </AppButton>
      </div>
    </form>
  </section>
</template>
