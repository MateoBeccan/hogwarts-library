<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import * as alquileresApi from '@/api/alquileres.api';
import * as clientesApi from '@/api/clientes.api';
import * as ejemplaresApi from '@/api/ejemplares.api';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { ApiError } from '@/types/api';
import type { AlquilerRequest } from '@/types/alquiler';
import type { ClienteResponse } from '@/types/cliente';
import type { EjemplarResponse } from '@/types/ejemplar';

const router = useRouter();

const clientes = ref<ClienteResponse[]>([]);
const ejemplares = ref<EjemplarResponse[]>([]);
const selectedClienteId = ref<number | null>(null);
const selectedEjemplarIds = ref<number[]>([]);
const currentStep = ref<1 | 2 | 3>(1);
const clienteSearch = ref('');
const ejemplarSearch = ref('');
const isLoading = ref(true);
const isSaving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const fieldErrors = ref<Record<string, string>>({});

const form = reactive<AlquilerRequest>({
  clienteId: null,
  fechaVencimiento: tomorrowIso(),
  ejemplarIds: [],
  observaciones: '',
});

const maxEjemplaresDto = 3;

const steps = [
  { value: 1, label: 'Cliente' },
  { value: 2, label: 'Ejemplares' },
  { value: 3, label: 'Confirmacion' },
] as const;

const selectedCliente = computed(() => clientes.value.find((cliente) => cliente.id === selectedClienteId.value) ?? null);

const filteredClientes = computed(() => {
  const term = clienteSearch.value.trim().toLowerCase();

  return clientes.value
    .filter((cliente) => cliente.activo)
    .filter((cliente) => {
      const searchable = `${cliente.nombre} ${cliente.apellido} ${cliente.dni}`.toLowerCase();
      return !term || searchable.includes(term);
    });
});

const rentableEjemplares = computed(() => ejemplares.value.filter((ejemplar) => ejemplar.estado === 'DISPONIBLE'));

const filteredEjemplares = computed(() => {
  const term = ejemplarSearch.value.trim().toLowerCase();

  return rentableEjemplares.value.filter((ejemplar) => {
    const searchable = `${ejemplar.codigo} ${ejemplar.libroTitulo} ${ejemplar.libroIsbn}`.toLowerCase();
    return !term || searchable.includes(term);
  });
});

const selectedEjemplares = computed(() =>
  selectedEjemplarIds.value
    .map((id) => ejemplares.value.find((ejemplar) => ejemplar.id === id))
    .filter((ejemplar): ejemplar is EjemplarResponse => Boolean(ejemplar)),
);

const estimatedDays = computed(() => calculateDays(form.fechaVencimiento));
const estimatedTotal = computed(() =>
  selectedEjemplares.value.reduce((total, ejemplar) => total + ejemplar.precioDia * estimatedDays.value, 0),
);

const canContinueFromCliente = computed(() => Boolean(selectedCliente.value));
const canContinueFromEjemplares = computed(() => selectedEjemplarIds.value.length > 0);
const canSubmit = computed(
  () => Boolean(selectedCliente.value) && selectedEjemplarIds.value.length > 0 && Boolean(form.fechaVencimiento) && !isSaving.value,
);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function tomorrowIso(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

function nombreCompleto(cliente: ClienteResponse): string {
  return `${cliente.nombre} ${cliente.apellido}`.trim();
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function calculateDays(fechaVencimiento: string): number {
  if (!fechaVencimiento) return 0;

  const start = new Date(`${todayIso()}T00:00:00`);
  const end = new Date(`${fechaVencimiento}T00:00:00`);
  const days = Math.ceil((end.getTime() - start.getTime()) / 86_400_000);

  return Math.max(days, 0);
}

function clearMessages(): void {
  errorMessage.value = '';
  successMessage.value = '';
  fieldErrors.value = {};
}

function selectCliente(cliente: ClienteResponse): void {
  selectedClienteId.value = cliente.id;
  fieldErrors.value.clienteId = '';
  successMessage.value = '';
}

function isEjemplarSelected(id: number): boolean {
  return selectedEjemplarIds.value.includes(id);
}

function toggleEjemplar(ejemplar: EjemplarResponse): void {
  successMessage.value = '';
  fieldErrors.value.ejemplarIds = '';

  if (isEjemplarSelected(ejemplar.id)) {
    selectedEjemplarIds.value = selectedEjemplarIds.value.filter((id) => id !== ejemplar.id);
    return;
  }

  if (selectedEjemplarIds.value.length >= maxEjemplaresDto) {
    errorMessage.value = 'El backend valida un maximo de 3 ejemplares por alquiler.';
    return;
  }

  errorMessage.value = '';
  selectedEjemplarIds.value = [...selectedEjemplarIds.value, ejemplar.id];
}

function removeEjemplar(id: number): void {
  selectedEjemplarIds.value = selectedEjemplarIds.value.filter((ejemplarId) => ejemplarId !== id);
}

function goToStep(step: 1 | 2 | 3): void {
  clearMessages();
  currentStep.value = step;
}

function goNext(): void {
  clearMessages();

  if (currentStep.value === 1 && !canContinueFromCliente.value) {
    fieldErrors.value = { clienteId: 'Seleccione un cliente activo.' };
    return;
  }

  if (currentStep.value === 2 && !canContinueFromEjemplares.value) {
    fieldErrors.value = { ejemplarIds: 'Seleccione al menos un ejemplar disponible.' };
    return;
  }

  currentStep.value = currentStep.value === 1 ? 2 : 3;
}

function goBack(): void {
  clearMessages();
  currentStep.value = currentStep.value === 3 ? 2 : 1;
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.data?.validationErrors) {
      fieldErrors.value = error.response.data.validationErrors;
    }

    if (error.response?.status === 403) return 'No tienes permisos para realizar esta operacion.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

function validateBeforeSubmit(): boolean {
  const errors: Record<string, string> = {};

  if (!selectedCliente.value) errors.clienteId = 'Seleccione un cliente activo.';
  if (selectedEjemplarIds.value.length === 0) errors.ejemplarIds = 'Seleccione al menos un ejemplar disponible.';
  if (!form.fechaVencimiento) errors.fechaVencimiento = 'La fecha de vencimiento es obligatoria.';
  if (form.fechaVencimiento && calculateDays(form.fechaVencimiento) <= 0) {
    errors.fechaVencimiento = 'La fecha de vencimiento debe ser futura.';
  }
  if (form.observaciones.length > 500) errors.observaciones = 'Las observaciones no pueden superar los 500 caracteres.';

  fieldErrors.value = errors;
  return Object.keys(errors).length === 0;
}

async function submitAlquiler(): Promise<void> {
  if (isSaving.value) return;

  clearMessages();

  if (!validateBeforeSubmit()) return;

  try {
    isSaving.value = true;

    const request: AlquilerRequest = {
      clienteId: selectedClienteId.value,
      fechaVencimiento: form.fechaVencimiento,
      ejemplarIds: [...selectedEjemplarIds.value],
      observaciones: form.observaciones.trim(),
    };

    const alquiler = await alquileresApi.crearAlquiler(request);
    successMessage.value = 'Alquiler creado correctamente.';
    await router.push({ path: `/alquileres/${alquiler.id}`, query: { created: '1' } });
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos crear el alquiler.');
  } finally {
    isSaving.value = false;
  }
}

async function loadInitialData(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    const [clientesResponse, ejemplaresResponse] = await Promise.all([
      clientesApi.listarClientes(),
      ejemplaresApi.listarEjemplares(),
    ]);
    clientes.value = clientesResponse;
    ejemplares.value = ejemplaresResponse;
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar los datos para crear el alquiler.');
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  void loadInitialData();
});
</script>

<template>
  <section>
    <PageHeader title="Nuevo alquiler" subtitle="Selecciona cliente, ejemplares y confirma las condiciones del alquiler.">
    </PageHeader>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="isLoading" class="form-skeleton" aria-label="Cargando datos del alquiler">
      <span v-for="row in 5" :key="row" />
    </div>

    <section v-else class="wizard-card">
      <nav class="wizard-steps" aria-label="Pasos de nuevo alquiler">
        <button
          v-for="step in steps"
          :key="step.value"
          type="button"
          class="wizard-step"
          :class="{ 'wizard-step--active': currentStep === step.value, 'wizard-step--done': currentStep > step.value }"
          @click="goToStep(step.value)"
        >
          <span>{{ step.value }}</span>
          {{ step.label }}
        </button>
      </nav>

      <section v-if="currentStep === 1" class="wizard-panel">
        <div class="section-heading section-heading--plain">
          <h2>Paso 1 - Cliente</h2>
          <p>Solo los clientes activos pueden seleccionarse para un alquiler nuevo.</p>
        </div>

        <AppInput id="cliente-search" v-model="clienteSearch" label="Buscar cliente" placeholder="Nombre, apellido o DNI..." />
        <p v-if="fieldErrors.clienteId" class="field__error">{{ fieldErrors.clienteId }}</p>

        <div v-if="filteredClientes.length === 0" class="empty-state empty-state--compact">
          <strong>No hay clientes activos</strong>
          <p>No se encontraron clientes seleccionables con la busqueda actual.</p>
        </div>

        <div v-else class="selection-grid">
          <button
            v-for="cliente in filteredClientes"
            :key="cliente.id"
            type="button"
            class="selection-card"
            :class="{ 'selection-card--selected': selectedClienteId === cliente.id }"
            @click="selectCliente(cliente)"
          >
            <span class="selection-card__title">{{ nombreCompleto(cliente) }}</span>
            <span>DNI {{ cliente.dni }}</span>
            <span>{{ cliente.email }}</span>
            <StatusBadge tone="success">Activo</StatusBadge>
          </button>
        </div>
      </section>

      <section v-else-if="currentStep === 2" class="wizard-panel">
        <div class="section-heading section-heading--plain">
          <h2>Paso 2 - Ejemplares</h2>
          <p>Se muestran ejemplares disponibles a este momento.</p>
        </div>

        <AppInput id="ejemplar-search" v-model="ejemplarSearch" label="Buscar ejemplar" placeholder="Codigo, titulo o ISBN..." />
        <p v-if="fieldErrors.ejemplarIds" class="field__error">{{ fieldErrors.ejemplarIds }}</p>

        <div class="wizard-columns">
          <div>
            <div v-if="filteredEjemplares.length === 0" class="empty-state empty-state--compact">
              <strong>No hay ejemplares disponibles</strong>
              <p>No se encontraron ejemplares alquilables con la busqueda actual.</p>
            </div>

            <div v-else class="selection-grid selection-grid--dense">
              <button
                v-for="ejemplar in filteredEjemplares"
                :key="ejemplar.id"
                type="button"
                class="selection-card"
                :class="{ 'selection-card--selected': isEjemplarSelected(ejemplar.id) }"
                @click="toggleEjemplar(ejemplar)"
              >
                <span class="selection-card__title">{{ ejemplar.codigo }}</span>
                <span>{{ ejemplar.libroTitulo }}</span>
                <span>ISBN {{ ejemplar.libroIsbn }}</span>
                <strong>{{ formatCurrency(ejemplar.precioDia) }} / dia</strong>
              </button>
            </div>
          </div>

          <aside class="selected-panel">
            <h3>Ejemplares del alquiler</h3>
            <p>{{ selectedEjemplares.length }} seleccionados</p>

            <div v-if="selectedEjemplares.length === 0" class="selected-panel__empty">
              Todavia no seleccionaste ejemplares.
            </div>

            <ul v-else class="selected-list">
              <li v-for="ejemplar in selectedEjemplares" :key="ejemplar.id">
                <div>
                  <strong>{{ ejemplar.codigo }}</strong>
                  <span>{{ ejemplar.libroTitulo }}</span>
                  <small>{{ formatCurrency(ejemplar.precioDia) }} / dia</small>
                </div>
                <button type="button" @click="removeEjemplar(ejemplar.id)">Quitar</button>
              </li>
            </ul>
          </aside>
        </div>
      </section>

      <section v-else class="wizard-panel">
        <div class="section-heading section-heading--plain">
          <h2>Paso 3 - Confirmacion</h2>
          <p>El calculo de importes es estimado.</p>
        </div>

        <div class="form-grid form-grid--single wizard-date-grid">
          <label class="field" for="fecha-vencimiento">
            <span class="field__label">Fecha de vencimiento</span>
            <input
              id="fecha-vencimiento"
              v-model="form.fechaVencimiento"
              class="field__control"
              :class="{ 'field__control--invalid': fieldErrors.fechaVencimiento }"
              type="date"
              :min="tomorrowIso()"
            />
            <span v-if="fieldErrors.fechaVencimiento" class="field__error">{{ fieldErrors.fechaVencimiento }}</span>
          </label>
          <label class="field" for="observaciones">
            <span class="field__label">Observaciones</span>
            <textarea
              id="observaciones"
              v-model="form.observaciones"
              class="field__control field__control--textarea"
              :class="{ 'field__control--invalid': fieldErrors.observaciones }"
              maxlength="500"
              placeholder="Notas internas del alquiler..."
            />
            <span v-if="fieldErrors.observaciones" class="field__error">{{ fieldErrors.observaciones }}</span>
          </label>
        </div>

        <div class="confirmation-grid">
          <section class="confirmation-card">
            <h3>Cliente</h3>
            <p v-if="selectedCliente">
              <strong>{{ nombreCompleto(selectedCliente) }}</strong>
              <span>DNI {{ selectedCliente.dni }}</span>
              <span>{{ selectedCliente.email }}</span>
            </p>
          </section>

          <section class="confirmation-card">
            <h3>Condiciones</h3>
            <p>
              <strong>{{ form.fechaVencimiento || '-' }}</strong>
              <span>{{ estimatedDays }} dias estimados</span>
              <span>{{ selectedEjemplares.length }} ejemplares</span>
            </p>
          </section>
        </div>

        <section class="data-card rental-section">
          <div class="table-wrap">
            <table class="app-table app-table--rental-details">
              <thead>
                <tr>
                  <th>Codigo</th>
                  <th>Libro</th>
                  <th>Precio diario</th>
                  <th>Dias</th>
                  <th>Subtotal estimado</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="ejemplar in selectedEjemplares" :key="ejemplar.id">
                  <td>{{ ejemplar.codigo }}</td>
                  <td>{{ ejemplar.libroTitulo }}</td>
                  <td>{{ formatCurrency(ejemplar.precioDia) }}</td>
                  <td>{{ estimatedDays }}</td>
                  <td>{{ formatCurrency(ejemplar.precioDia * estimatedDays) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="financial-summary financial-summary--wizard">
          <div>
            <span>Total estimado</span>
            <strong>{{ formatCurrency(estimatedTotal) }}</strong>
          </div>
        </section>
      </section>

      <footer class="wizard-actions">
        <AppButton v-if="currentStep > 1" variant="ghost" @click="goBack">Volver</AppButton>
        <RouterLink class="button-link button-link--ghost" to="/alquileres">Cancelar</RouterLink>
        <AppButton v-if="currentStep < 3" :disabled="currentStep === 1 ? !canContinueFromCliente : !canContinueFromEjemplares" @click="goNext">
          Continuar
        </AppButton>
        <AppButton v-else :loading="isSaving" :disabled="!canSubmit" @click="submitAlquiler">Confirmar alquiler</AppButton>
      </footer>
    </section>
  </section>
</template>
