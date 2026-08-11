<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import * as autoresApi from '@/api/autores.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { ApiError } from '@/types/api';
import type { AutorRequest } from '@/types/autor';

const route = useRoute();
const router = useRouter();

const form = reactive<AutorRequest>({
  nombreCompleto: '',
});

const fieldErrors = reactive<Record<keyof AutorRequest, string>>({
  nombreCompleto: '',
});

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref('');

const autorId = computed(() => {
  const rawId = route.params.id;
  return typeof rawId === 'string' ? Number(rawId) : null;
});
const isEditing = computed(() => Number.isFinite(autorId.value));
const pageTitle = computed(() => (isEditing.value ? 'Editar autor' : 'Nuevo autor'));

function resetFieldErrors(): void {
  fieldErrors.nombreCompleto = '';
}

function validate(): boolean {
  resetFieldErrors();

  const nombreCompleto = form.nombreCompleto.trim();

  if (!nombreCompleto) fieldErrors.nombreCompleto = 'El nombre completo es obligatorio';
  if (nombreCompleto.length > 150) fieldErrors.nombreCompleto = 'El nombre no puede superar los 150 caracteres';

  return !Object.values(fieldErrors).some(Boolean);
}

function applyValidationErrors(errors: Record<string, string>): void {
  Object.entries(errors).forEach(([field, message]) => {
    if (field in fieldErrors) {
      fieldErrors[field as keyof AutorRequest] = message;
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

async function loadAutor(): Promise<void> {
  if (!isEditing.value || autorId.value === null) {
    return;
  }

  try {
    isLoading.value = true;
    errorMessage.value = '';
    const autor = await autoresApi.buscarAutorPorId(autorId.value);
    form.nombreCompleto = autor.nombreCompleto;
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el autor.');
  } finally {
    isLoading.value = false;
  }
}

async function submit(): Promise<void> {
  if (isSubmitting.value || !validate()) {
    return;
  }

  const request: AutorRequest = {
    nombreCompleto: form.nombreCompleto.trim(),
  };

  try {
    isSubmitting.value = true;
    errorMessage.value = '';
    resetFieldErrors();

    if (isEditing.value && autorId.value !== null) {
      await autoresApi.actualizarAutor(autorId.value, request);
      await router.push({ path: '/autores', query: { saved: 'updated' } });
      return;
    }

    await autoresApi.crearAutor(request);
    await router.push({ path: '/autores', query: { saved: 'created' } });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos guardar el autor.');
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(() => {
  void loadAutor();
});
</script>

<template>
  <section>
    <PageHeader :title="pageTitle" subtitle="Registra el nombre completo que se asociara a los libros." />

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton form-skeleton--compact" aria-label="Cargando autor">
      <span v-for="row in 2" :key="row" />
    </div>

    <form v-else class="form-card form-card--compact" @submit.prevent="submit">
      <section class="form-section">
        <h2>Datos de autor</h2>
        <div class="form-grid form-grid--single">
          <AppInput
            id="nombreCompleto"
            v-model="form.nombreCompleto"
            label="Nombre completo"
            :error="fieldErrors.nombreCompleto"
            autofocus
          />
        </div>
      </section>

      <div class="form-actions">
        <RouterLink class="button-link button-link--ghost" to="/autores">Cancelar</RouterLink>
        <AppButton type="submit" :loading="isSubmitting" :disabled="isSubmitting">
          Guardar autor
        </AppButton>
      </div>
    </form>
  </section>
</template>