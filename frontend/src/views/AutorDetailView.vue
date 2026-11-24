<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as autoresApi from '@/api/autores.api';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { AutorResponse } from '@/types/autor';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const route = useRoute();

const autor = ref<AutorResponse | null>(null);
const isLoading = ref(true);
const errorMessage = ref('');

const autorId = computed(() => Number(route.params.id));

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 404) {
      return 'No encontramos el autor solicitado.';
    }

    if (error.response?.status === 403) {
      return 'No tienes permisos para ver este recurso.';
    }

    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

async function loadAutor(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    autor.value = await autoresApi.buscarAutorPorId(autorId.value);
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el autor.');
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  void loadAutor();
});
</script>

<template>
  <section>
    <PageHeader title="Detalle de autor" subtitle="Informacion del autor seleccionado.">
      <div class="header-actions">
        <RouterLink class="button-link button-link--ghost" to="/autores">Volver</RouterLink>
        <RouterLink v-if="!authStore.isDemo && autor" class="button-link button-link--primary" :to="`/autores/${autor.id}/editar`">Editar</RouterLink>
      </div>
    </PageHeader>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="detail-skeleton" aria-label="Cargando autor">
      <span v-for="row in 3" :key="row" />
    </div>

    <article v-else-if="autor" class="detail-card author-detail-card">
      <div class="detail-card__header">
        <div>
          <p class="eyebrow">Autor</p>
          <h2>{{ autor.nombreCompleto }}</h2>
        </div>
        <StatusBadge :tone="autor.activo ? 'success' : 'danger'">
          {{ autor.activo ? 'ACTIVO' : 'INACTIVO' }}
        </StatusBadge>
      </div>

      <dl class="detail-grid detail-grid--compact">
        <div>
          <dt>Nombre completo</dt>
          <dd>{{ autor.nombreCompleto }}</dd>
        </div>
        <div>
          <dt>Estado</dt>
          <dd>{{ autor.activo ? 'Activo' : 'Inactivo' }}</dd>
        </div>
      </dl>
    </article>

    <div v-else class="empty-state">
      <strong>No encontramos el autor</strong>
      <p>Vuelve al listado para seleccionar otro registro.</p>
      <RouterLink class="button-link button-link--primary" to="/autores">Ir a autores</RouterLink>
    </div>
  </section>
</template>