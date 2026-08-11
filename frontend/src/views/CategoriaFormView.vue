<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import * as categoriasApi from '@/api/categorias.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { ApiError } from '@/types/api';
import type { CategoriaRequest } from '@/types/categoria';

const route = useRoute();
const router = useRouter();

const form = reactive<CategoriaRequest>({
  nombre: '',
  descripcion: '',
});

const fieldErrors = reactive<Record<keyof CategoriaRequest, string>>({
  nombre: '',
  descripcion: '',
});

const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref('');

const categoriaId = computed(() => {
  const rawId = route.params.id;
  return typeof rawId === 'string' ? Number(rawId) : null;
});
const isEditing = computed(() => Number.isFinite(categoriaId.value));
const pageTitle = computed(() => (isEditing.value ? 'Editar categoria' : 'Nueva categoria'));

function resetFieldErrors(): void {
  fieldErrors.nombre = '';
  fieldErrors.descripcion = '';
}

function validate(): boolean {
  resetFieldErrors();

  if (!form.nombre.trim()) fieldErrors.nombre = 'El nombre de la categoria es obligatorio';
  if (form.nombre.length > 100) fieldErrors.nombre = 'El nombre no puede superar los 100 caracteres';
  if (form.descripcion.length > 500) fieldErrors.descripcion = 'La descripcion no puede superar los 500 caracteres';

  return !Object.values(fieldErrors).some(Boolean);
}

function applyValidationErrors(errors: Record<string, string>): void {
  Object.entries(errors).forEach(([field, message]) => {
    if (field in fieldErrors) {
      fieldErrors[field as keyof CategoriaRequest] = message;
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

async function loadCategoria(): Promise<void> {
  if (!isEditing.value || categoriaId.value === null) {
    return;
  }

  try {
    isLoading.value = true;
    errorMessage.value = '';
    const categoria = await categoriasApi.buscarCategoriaPorId(categoriaId.value);
    form.nombre = categoria.nombre;
    form.descripcion = categoria.descripcion ?? '';
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar la categoria.');
  } finally {
    isLoading.value = false;
  }
}

async function submit(): Promise<void> {
  if (isSubmitting.value || !validate()) {
    return;
  }

  const request: CategoriaRequest = {
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim(),
  };

  try {
    isSubmitting.value = true;
    errorMessage.value = '';
    resetFieldErrors();

    if (isEditing.value && categoriaId.value !== null) {
      await categoriasApi.actualizarCategoria(categoriaId.value, request);
      await router.push({ path: '/categorias', query: { saved: 'updated' } });
      return;
    }

    await categoriasApi.crearCategoria(request);
    await router.push({ path: '/categorias', query: { saved: 'created' } });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos guardar la categoria.');
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(() => {
  void loadCategoria();
});
</script>

<template>
  <section>
    <PageHeader :title="pageTitle" subtitle="Define el nombre y una descripcion breve para clasificar libros." />

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton form-skeleton--compact" aria-label="Cargando categoria">
      <span v-for="row in 3" :key="row" />
    </div>

    <form v-else class="form-card form-card--compact" @submit.prevent="submit">
      <section class="form-section">
        <h2>Datos de categoria</h2>
        <div class="form-grid form-grid--single">
          <AppInput id="nombre" v-model="form.nombre" label="Nombre" :error="fieldErrors.nombre" />

          <label class="field" for="descripcion">
            <span class="field__label">Descripcion</span>
            <textarea
              id="descripcion"
              v-model="form.descripcion"
              class="field__control field__control--textarea"
              :class="{ 'field__control--invalid': fieldErrors.descripcion }"
              rows="5"
            />
            <span v-if="fieldErrors.descripcion" class="field__error">{{ fieldErrors.descripcion }}</span>
          </label>
        </div>
      </section>

      <div class="form-actions">
        <RouterLink class="button-link button-link--ghost" to="/categorias">Cancelar</RouterLink>
        <AppButton type="submit" :loading="isSubmitting" :disabled="isSubmitting">
          Guardar categoria
        </AppButton>
      </div>
    </form>
  </section>
</template>
