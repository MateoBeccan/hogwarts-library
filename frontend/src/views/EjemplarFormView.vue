<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import * as ejemplaresApi from '@/api/ejemplares.api';
import * as librosApi from '@/api/libros.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { ApiError } from '@/types/api';
import type { EjemplarRequest } from '@/types/ejemplar';
import type { LibroResponse } from '@/types/libro';
import { formatAutores } from '@/types/libro';

interface EjemplarFormState {
  precioDia: string;
  observaciones: string;
  libroId: string;
}

const route = useRoute();
const router = useRouter();

const form = reactive<EjemplarFormState>({
  precioDia: '',
  observaciones: '',
  libroId: '',
});

const fieldErrors = reactive<Record<keyof EjemplarFormState, string>>({
  precioDia: '',
  observaciones: '',
  libroId: '',
});

const libros = ref<LibroResponse[]>([]);
const codigoActual = ref('');
const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref('');

const ejemplarId = computed(() => {
  const rawId = route.params.id;
  return typeof rawId === 'string' ? Number(rawId) : null;
});
const isEditing = computed(() => Number.isFinite(ejemplarId.value));
const pageTitle = computed(() => (isEditing.value ? 'Editar ejemplar' : 'Nuevo ejemplar'));
const librosActivos = computed(() => libros.value.filter((libro) => libro.activo));

function resetFieldErrors(): void {
  fieldErrors.precioDia = '';
  fieldErrors.observaciones = '';
  fieldErrors.libroId = '';
}

function validate(): boolean {
  resetFieldErrors();

  if (!form.precioDia) {
    fieldErrors.precioDia = 'El precio por dia es obligatorio';
  } else {
    const precio = Number(form.precioDia);
    if (!Number.isFinite(precio) || precio < 0.01) fieldErrors.precioDia = 'El precio por dia debe ser mayor a 0';
    if (!/^\d{1,8}(\.\d{1,2})?$/.test(form.precioDia)) {
      fieldErrors.precioDia = 'El precio por dia debe tener como maximo 8 enteros y 2 decimales';
    }
  }

  if (form.observaciones.length > 500) fieldErrors.observaciones = 'Las observaciones no pueden superar los 500 caracteres';

  if (!form.libroId) fieldErrors.libroId = 'El libro es obligatorio';
  if (form.libroId && Number(form.libroId) <= 0) fieldErrors.libroId = 'El id del libro debe ser mayor a 0';

  return !Object.values(fieldErrors).some(Boolean);
}

function applyValidationErrors(errors: Record<string, string>): void {
  Object.entries(errors).forEach(([field, message]) => {
    if (field in fieldErrors) {
      fieldErrors[field as keyof EjemplarFormState] = message;
    }
  });
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.data?.validationErrors) applyValidationErrors(error.response.data.validationErrors);
    if (error.response?.status === 403) return 'No tienes permisos para realizar esta operacion.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

async function loadInitialData(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    libros.value = await librosApi.listarLibros();

    if (isEditing.value && ejemplarId.value !== null) {
      const ejemplar = await ejemplaresApi.buscarEjemplarPorId(ejemplarId.value);
      codigoActual.value = ejemplar.codigo;
      form.precioDia = String(ejemplar.precioDia);
      form.observaciones = ejemplar.observaciones ?? '';
      form.libroId = String(ejemplar.libroId);
    }
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los datos del formulario.');
  } finally {
    isLoading.value = false;
  }
}

function buildRequest(): EjemplarRequest {
  return {
    precioDia: form.precioDia ? Number(form.precioDia) : null,
    observaciones: form.observaciones.trim(),
    libroId: form.libroId ? Number(form.libroId) : null,
  };
}

async function submit(): Promise<void> {
  if (isSubmitting.value || !validate()) return;

  try {
    isSubmitting.value = true;
    errorMessage.value = '';
    resetFieldErrors();

    const request = buildRequest();
    const saved = isEditing.value && ejemplarId.value !== null
      ? await ejemplaresApi.actualizarEjemplar(ejemplarId.value, request)
      : await ejemplaresApi.crearEjemplar(request);

    await router.push({
      path: `/ejemplares/${saved.id}`,
      query: { saved: isEditing.value ? 'updated' : 'created' },
    });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos guardar el ejemplar.');
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(() => {
  void loadInitialData();
});
</script>

<template>
  <section>
    <PageHeader :title="pageTitle" subtitle="Asocia una copia fisica a un libro activo." />

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton" aria-label="Cargando ejemplar">
      <span v-for="row in 4" :key="row" />
    </div>

    <form v-else class="form-card" @submit.prevent="submit">
      <section class="form-section">
        <h2>Datos del ejemplar</h2>
        <div v-if="isEditing && codigoActual" class="readonly-field">
          <span class="readonly-field__label">Codigo del ejemplar</span>
          <strong>{{ codigoActual }}</strong>
        </div>
        <p v-else class="form-hint">El codigo del ejemplar se generara automaticamente.</p>
        <div class="form-grid">
          <AppInput id="precioDia" v-model="form.precioDia" label="Precio por dia" type="number" :error="fieldErrors.precioDia" />
          <label class="field" for="libroId">
            <span class="field__label">Libro</span>
            <select
              id="libroId"
              v-model="form.libroId"
              class="field__control"
              :class="{ 'field__control--invalid': fieldErrors.libroId }"
            >
              <option value="">Seleccionar libro...</option>
              <option v-for="libro in librosActivos" :key="libro.id" :value="String(libro.id)">
                {{ libro.titulo }} - {{ formatAutores(libro.autores) }}
              </option>
            </select>
            <span v-if="fieldErrors.libroId" class="field__error">{{ fieldErrors.libroId }}</span>
          </label>
        </div>
      </section>

      <section class="form-section">
        <h2>Observaciones</h2>
        <label class="field" for="observaciones">
          <span class="field__label">Observaciones</span>
          <textarea
            id="observaciones"
            v-model="form.observaciones"
            class="field__control field__control--textarea"
            :class="{ 'field__control--invalid': fieldErrors.observaciones }"
            rows="5"
          />
          <span v-if="fieldErrors.observaciones" class="field__error">{{ fieldErrors.observaciones }}</span>
        </label>
      </section>

      <div class="form-actions">
        <RouterLink class="button-link button-link--ghost" to="/ejemplares">Cancelar</RouterLink>
        <AppButton type="submit" :loading="isSubmitting" :disabled="isSubmitting">
          Guardar ejemplar
        </AppButton>
      </div>
    </form>
  </section>
</template>