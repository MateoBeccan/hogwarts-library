<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import * as autoresApi from '@/api/autores.api';
import * as categoriasApi from '@/api/categorias.api';
import * as librosApi from '@/api/libros.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { AutorResponse } from '@/types/autor';
import type { CategoriaResponse } from '@/types/categoria';
import type { LibroRequest } from '@/types/libro';

interface LibroFormState {
  isbn: string;
  titulo: string;
  editorial: string;
  anioPublicacion: string;
  descripcion: string;
  categoriaId: string;
  autorIds: number[];
}

const route = useRoute();
const router = useRouter();

const form = reactive<LibroFormState>({
  isbn: '',
  titulo: '',
  editorial: '',
  anioPublicacion: '',
  descripcion: '',
  categoriaId: '',
  autorIds: [],
});

const fieldErrors = reactive<Record<keyof LibroFormState, string>>({
  isbn: '',
  titulo: '',
  editorial: '',
  anioPublicacion: '',
  descripcion: '',
  categoriaId: '',
  autorIds: '',
});

const categorias = ref<CategoriaResponse[]>([]);
const autores = ref<AutorResponse[]>([]);
const authorSearch = ref('');
const isLoading = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref('');

const libroId = computed(() => {
  const rawId = route.params.id;
  return typeof rawId === 'string' ? Number(rawId) : null;
});
const isEditing = computed(() => Number.isFinite(libroId.value));
const pageTitle = computed(() => (isEditing.value ? 'Editar libro' : 'Nuevo libro'));
const categoriasActivas = computed(() => categorias.value.filter((categoria) => categoria.activa));

const autoresAsignables = computed(() => {
  const selectedIds = new Set(form.autorIds);
  const term = authorSearch.value.trim().toLowerCase();

  return autores.value
    .filter((autor) => autor.activo || selectedIds.has(autor.id))
    .filter((autor) => !term || autor.nombreCompleto.toLowerCase().includes(term));
});

const autoresSeleccionados = computed(() => {
  const selectedIds = new Set(form.autorIds);
  return autores.value.filter((autor) => selectedIds.has(autor.id));
});

function resetFieldErrors(): void {
  fieldErrors.isbn = '';
  fieldErrors.titulo = '';
  fieldErrors.editorial = '';
  fieldErrors.anioPublicacion = '';
  fieldErrors.descripcion = '';
  fieldErrors.categoriaId = '';
  fieldErrors.autorIds = '';
}

function validate(): boolean {
  resetFieldErrors();

  if (!form.isbn.trim()) fieldErrors.isbn = 'El ISBN es obligatorio';
  if (form.isbn.length > 20) fieldErrors.isbn = 'El ISBN no puede superar los 20 caracteres';

  if (!form.titulo.trim()) fieldErrors.titulo = 'El titulo es obligatorio';
  if (form.titulo.length > 200) fieldErrors.titulo = 'El titulo no puede superar los 200 caracteres';

  if (form.autorIds.length === 0) fieldErrors.autorIds = 'Selecciona al menos un autor';

  if (form.editorial.length > 150) fieldErrors.editorial = 'La editorial no puede superar los 150 caracteres';

  if (form.anioPublicacion) {
    const anio = Number(form.anioPublicacion);
    if (!Number.isInteger(anio) || anio <= 0) {
      fieldErrors.anioPublicacion = 'El anio de publicacion debe ser mayor a 0';
    }
  }

  if (form.descripcion.length > 1000) fieldErrors.descripcion = 'La descripcion no puede superar los 1000 caracteres';

  if (!form.categoriaId) fieldErrors.categoriaId = 'La categoria es obligatoria';
  if (form.categoriaId && Number(form.categoriaId) <= 0) fieldErrors.categoriaId = 'El id de la categoria debe ser mayor a 0';

  return !Object.values(fieldErrors).some(Boolean);
}

function applyValidationErrors(errors: Record<string, string>): void {
  Object.entries(errors).forEach(([field, message]) => {
    if (field in fieldErrors) {
      fieldErrors[field as keyof LibroFormState] = message;
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

async function loadInitialData(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';

    const [categoriasResponse, autoresResponse] = await Promise.all([
      categoriasApi.listarCategorias(),
      autoresApi.listarAutores(),
    ]);

    categorias.value = categoriasResponse;
    autores.value = autoresResponse;

    if (isEditing.value && libroId.value !== null) {
      const libro = await librosApi.buscarLibroPorId(libroId.value);
      form.isbn = libro.isbn;
      form.titulo = libro.titulo;
      form.editorial = libro.editorial ?? '';
      form.anioPublicacion = libro.anioPublicacion ? String(libro.anioPublicacion) : '';
      form.descripcion = libro.descripcion ?? '';
      form.categoriaId = libro.categoriaId ? String(libro.categoriaId) : '';
      form.autorIds = libro.autores.map((autor) => autor.id);
    }
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los datos del formulario.');
  } finally {
    isLoading.value = false;
  }
}

function toggleAutor(autorId: number): void {
  if (form.autorIds.includes(autorId)) {
    form.autorIds = form.autorIds.filter((id) => id !== autorId);
    return;
  }

  form.autorIds = [...form.autorIds, autorId];
}

function buildRequest(): LibroRequest {
  return {
    isbn: form.isbn.trim(),
    titulo: form.titulo.trim(),
    autorIds: [...new Set(form.autorIds)],
    editorial: form.editorial.trim(),
    anioPublicacion: form.anioPublicacion ? Number(form.anioPublicacion) : null,
    descripcion: form.descripcion.trim(),
    categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
  };
}

async function submit(): Promise<void> {
  if (isSubmitting.value || !validate()) {
    return;
  }

  try {
    isSubmitting.value = true;
    errorMessage.value = '';
    resetFieldErrors();

    const request = buildRequest();
    const saved = isEditing.value && libroId.value !== null
      ? await librosApi.actualizarLibro(libroId.value, request)
      : await librosApi.crearLibro(request);

    await router.push({
      path: `/libros/${saved.id}`,
      query: { saved: isEditing.value ? 'updated' : 'created' },
    });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos guardar el libro.');
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
    <PageHeader :title="pageTitle" subtitle="Completa los datos editoriales, autores y categoria activa." />

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton" aria-label="Cargando libro">
      <span v-for="row in 6" :key="row" />
    </div>

    <form v-else class="form-card" @submit.prevent="submit">
      <section class="form-section">
        <h2>Datos principales</h2>
        <div class="form-grid">
          <AppInput id="titulo" v-model="form.titulo" label="Titulo" :error="fieldErrors.titulo" />
          <AppInput id="isbn" v-model="form.isbn" label="ISBN" :error="fieldErrors.isbn" />
          <label class="field" for="categoriaId">
            <span class="field__label">Categoria</span>
            <select
              id="categoriaId"
              v-model="form.categoriaId"
              class="field__control"
              :class="{ 'field__control--invalid': fieldErrors.categoriaId }"
            >
              <option value="">Seleccionar categoria...</option>
              <option v-for="categoria in categoriasActivas" :key="categoria.id" :value="String(categoria.id)">
                {{ categoria.nombre }}
              </option>
            </select>
            <span v-if="fieldErrors.categoriaId" class="field__error">{{ fieldErrors.categoriaId }}</span>
          </label>
        </div>
      </section>

      <section class="form-section">
        <div class="section-heading section-heading--plain">
          <h2>Autores</h2>
          <p>Selecciona uno o varios autores activos para esta obra.</p>
        </div>

        <AppInput id="author-search" v-model="authorSearch" label="Buscar autor" placeholder="Buscar por nombre..." />

        <div v-if="fieldErrors.autorIds" class="field__error" role="alert">{{ fieldErrors.autorIds }}</div>

        <div v-if="autoresAsignables.length === 0" class="empty-state empty-state--compact author-picker-empty">
          <strong>No hay autores disponibles</strong>
          <p>Registra autores activos antes de asociarlos a un libro.</p>
        </div>

        <div v-else class="author-picker">
          <label
            v-for="autor in autoresAsignables"
            :key="autor.id"
            class="author-option"
            :class="{ 'author-option--selected': form.autorIds.includes(autor.id), 'author-option--inactive': !autor.activo }"
          >
            <input
              type="checkbox"
              :checked="form.autorIds.includes(autor.id)"
              :disabled="!autor.activo && !form.autorIds.includes(autor.id)"
              @change="toggleAutor(autor.id)"
            />
            <span>
              <strong>{{ autor.nombreCompleto }}</strong>
              <StatusBadge :tone="autor.activo ? 'success' : 'danger'">
                {{ autor.activo ? 'ACTIVO' : 'INACTIVO' }}
              </StatusBadge>
            </span>
          </label>
        </div>

        <div v-if="autoresSeleccionados.length" class="author-chips" aria-label="Autores seleccionados">
          <span v-for="autor in autoresSeleccionados" :key="autor.id" class="author-chip">
            {{ autor.nombreCompleto }}
          </span>
        </div>
      </section>

      <section class="form-section">
        <h2>Publicacion</h2>
        <div class="form-grid">
          <AppInput id="editorial" v-model="form.editorial" label="Editorial" :error="fieldErrors.editorial" />
          <AppInput
            id="anioPublicacion"
            v-model="form.anioPublicacion"
            label="Anio de publicacion"
            type="number"
            :error="fieldErrors.anioPublicacion"
          />
        </div>
      </section>

      <section class="form-section">
        <h2>Descripcion</h2>
        <label class="field" for="descripcion">
          <span class="field__label">Descripcion</span>
          <textarea
            id="descripcion"
            v-model="form.descripcion"
            class="field__control field__control--textarea"
            :class="{ 'field__control--invalid': fieldErrors.descripcion }"
            rows="6"
          />
          <span v-if="fieldErrors.descripcion" class="field__error">{{ fieldErrors.descripcion }}</span>
        </label>
      </section>

      <div class="form-actions">
        <RouterLink class="button-link button-link--ghost" to="/libros">Cancelar</RouterLink>
        <AppButton type="submit" :loading="isSubmitting" :disabled="isSubmitting">
          Guardar libro
        </AppButton>
      </div>
    </form>
  </section>
</template>