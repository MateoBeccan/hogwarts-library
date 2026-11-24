<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import * as alquileresApi from '@/api/alquileres.api';
import * as pagosApi from '@/api/pagos.api';
import AppButton from '@/components/common/AppButton.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useToastStore } from '@/stores/toast.store';
import type { ApiError } from '@/types/api';
import type { AlquilerResponse, EstadoAlquiler } from '@/types/alquiler';
import type { MetodoPago, PagoRequest, PagoResponse } from '@/types/pago';
import { useAuthStore } from '@/stores/auth.store';

const authStore = useAuthStore();
const route = useRoute();
const toastStore = useToastStore();

type RentalAction = 'finalizar' | 'cancelar';

const alquiler = ref<AlquilerResponse | null>(null);
const pagos = ref<PagoResponse[]>([]);
const isLoading = ref(true);
const isPaymentsLoading = ref(true);
const isActionLoading = ref(false);
const isPaymentDialogOpen = ref(false);
const isPaymentSaving = ref(false);
const pendingAction = ref<RentalAction | null>(null);
const errorMessage = ref('');
const paymentsErrorMessage = ref('');
const paymentErrorMessage = ref('');
const successMessage = ref(route.query.created === '1' ? 'Alquiler creado correctamente.' : '');
const paymentFieldErrors = ref<Record<string, string>>({});

const paymentForm = reactive<PagoRequest>({
  alquilerId: 0,
  monto: '',
  metodoPago: 'EFECTIVO',
  observaciones: '',
});

const metodoPagoOptions: MetodoPago[] = ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'];

const alquilerId = computed(() => Number(route.params.id));
const isActionDialogOpen = computed(() => Boolean(pendingAction.value));
const activeAction = computed(() => pendingAction.value);

const paymentSummary = computed(() => {
  const lastPayment = pagos.value.length > 0 ? pagos.value[pagos.value.length - 1] : null;
  const totalAlquiler = lastPayment?.totalAlquiler ?? alquiler.value?.total ?? 0;
  const totalPagado = lastPayment?.totalPagado ?? 0;
  const saldoPendiente = lastPayment?.saldoPendiente ?? totalAlquiler;

  return { totalAlquiler, totalPagado, saldoPendiente };
});

const canRegisterPayment = computed(() => {
  if (!alquiler.value || isPaymentsLoading.value || paymentsErrorMessage.value) return false;
  return alquiler.value.estado !== 'CANCELADO' && paymentSummary.value.saldoPendiente > 0;
});

const actionCopy = computed(() => {
  if (activeAction.value === 'cancelar') {
    return {
      title: 'Cancelar alquiler?',
      message: 'Los ejemplares asociados volveran a estar disponibles.',
      confirmLabel: 'Cancelar alquiler',
      cancelLabel: 'Volver',
      destructive: true,
    };
  }

  return {
    title: 'Finalizar alquiler?',
    message: 'Los ejemplares asociados volveran a estar disponibles.',
    confirmLabel: 'Finalizar alquiler',
    cancelLabel: 'Cancelar',
    destructive: false,
  };
});

function statusTone(estado: EstadoAlquiler): 'success' | 'warning' | 'danger' {
  if (estado === 'ACTIVO') return 'warning';
  if (estado === 'FINALIZADO') return 'success';
  return 'danger';
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function resolveError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.response?.status === 403) return 'No tienes permisos para realizar esta operacion.';
    if (error.response?.status === 404) return error.response?.data?.message ?? 'No encontramos el alquiler solicitado.';
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
}

function normalizeAmount(value: string): string {
  const trimmed = value.trim();
  if (trimmed.includes(',') && !trimmed.includes('.')) return trimmed.replace(',', '.');
  return trimmed;
}

function parsePaymentAmount(value: string): number | null {
  const normalized = normalizeAmount(value);
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) return null;

  const amount = Number(normalized);
  return Number.isFinite(amount) ? amount : null;
}

function resetPaymentForm(): void {
  paymentForm.alquilerId = alquilerId.value;
  paymentForm.monto = '';
  paymentForm.metodoPago = 'EFECTIVO';
  paymentForm.observaciones = '';
  paymentFieldErrors.value = {};
  paymentErrorMessage.value = '';
}

function openPaymentDialog(): void {
  if (!canRegisterPayment.value || isPaymentSaving.value) return;

  successMessage.value = '';
  errorMessage.value = '';
  resetPaymentForm();
  isPaymentDialogOpen.value = true;
}

function closePaymentDialog(): void {
  if (isPaymentSaving.value) return;
  isPaymentDialogOpen.value = false;
}

function validatePaymentForm(): boolean {
  const errors: Record<string, string> = {};
  const amount = parsePaymentAmount(paymentForm.monto);

  if (amount === null) {
    errors.monto = 'Ingrese un monto valido con hasta 2 decimales.';
  } else if (amount <= 0) {
    errors.monto = 'El monto debe ser mayor a 0.';
  } else if (amount > paymentSummary.value.saldoPendiente) {
    errors.monto = `El monto no puede superar el saldo pendiente de ${formatCurrency(paymentSummary.value.saldoPendiente)}.`;
  }

  if (!paymentForm.metodoPago) errors.metodoPago = 'El metodo de pago es obligatorio.';
  if (paymentForm.observaciones.length > 500) errors.observaciones = 'Las observaciones no pueden superar los 500 caracteres.';

  paymentFieldErrors.value = errors;
  return Object.keys(errors).length === 0;
}

function openAction(action: RentalAction): void {
  if (!alquiler.value || alquiler.value.estado !== 'ACTIVO' || isActionLoading.value) return;

  errorMessage.value = '';
  successMessage.value = '';
  pendingAction.value = action;
}

function closeActionDialog(): void {
  if (isActionLoading.value) return;
  pendingAction.value = null;
}

async function executePendingAction(): Promise<void> {
  if (!pendingAction.value || isActionLoading.value || !alquiler.value) return;

  const action = pendingAction.value;

  try {
    isActionLoading.value = true;
    errorMessage.value = '';
    successMessage.value = '';

    alquiler.value = action === 'finalizar'
      ? await alquileresApi.finalizarAlquiler(alquilerId.value)
      : await alquileresApi.cancelarAlquiler(alquilerId.value);

    successMessage.value = action === 'finalizar'
      ? 'Alquiler finalizado correctamente'
      : 'Alquiler cancelado correctamente';
    pendingAction.value = null;
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos actualizar el alquiler.');
  } finally {
    isActionLoading.value = false;
  }
}

async function submitPayment(): Promise<void> {
  if (isPaymentSaving.value) return;

  paymentErrorMessage.value = '';
  paymentFieldErrors.value = {};

  if (!validatePaymentForm()) return;

  try {
    isPaymentSaving.value = true;

    const request: PagoRequest = {
      alquilerId: alquilerId.value,
      monto: normalizeAmount(paymentForm.monto),
      metodoPago: paymentForm.metodoPago,
      observaciones: paymentForm.observaciones.trim(),
    };

    await pagosApi.registrarPago(request);
    isPaymentDialogOpen.value = false;
    toastStore.success('Pago registrado correctamente');
    await loadPagos();
  } catch (error) {
    if (axios.isAxiosError<ApiError>(error) && error.response?.data?.validationErrors) {
      paymentFieldErrors.value = error.response.data.validationErrors;
    }
    paymentErrorMessage.value = resolveError(error, 'No pudimos registrar el pago.');
  } finally {
    isPaymentSaving.value = false;
  }
}

async function loadAlquiler(): Promise<void> {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    alquiler.value = await alquileresApi.buscarAlquilerPorId(alquilerId.value);
  } catch (error) {
    errorMessage.value = resolveError(error, 'No pudimos cargar el alquiler.');
  } finally {
    isLoading.value = false;
  }
}

async function loadPagos(): Promise<void> {
  try {
    isPaymentsLoading.value = true;
    paymentsErrorMessage.value = '';
    pagos.value = await pagosApi.listarPagosPorAlquiler(alquilerId.value);
  } catch (error) {
    paymentsErrorMessage.value = resolveError(error, 'No pudimos cargar los pagos del alquiler.');
  } finally {
    isPaymentsLoading.value = false;
  }
}

onMounted(() => {
  void loadAlquiler();
  void loadPagos();
});
</script>

<template>
  <section>
    <PageHeader title="Detalle de alquiler" subtitle="Consulta los datos operativos y financieros del alquiler.">
      <RouterLink class="button-link button-link--ghost" to="/alquileres">Volver</RouterLink>
    </PageHeader>

    <div v-if="successMessage" class="alert alert--success" role="status">
      {{ successMessage }}
    </div>

    <div v-if="errorMessage" class="alert alert--danger" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="detail-skeleton" aria-label="Cargando alquiler">
      <span v-for="row in 6" :key="row" />
    </div>

    <article v-else-if="alquiler" class="rental-detail">
      <section class="detail-card">
        <div class="detail-card__header">
          <div>
            <p class="eyebrow">Alquiler</p>
            <h2>#{{ alquiler.id }}</h2>
          </div>
          <StatusBadge :tone="statusTone(alquiler.estado)">{{ alquiler.estado }}</StatusBadge>
        </div>

        <dl class="detail-grid">
          <div>
            <dt>Cliente</dt>
            <dd>{{ alquiler.clienteNombreCompleto }}</dd>
          </div>
          <div>
            <dt>ID cliente</dt>
            <dd>#{{ alquiler.clienteId }}</dd>
          </div>
          <div>
            <dt>Fecha alquiler</dt>
            <dd>{{ alquiler.fechaAlquiler }}</dd>
          </div>
          <div>
            <dt>Vencimiento</dt>
            <dd>{{ alquiler.fechaVencimiento }}</dd>
          </div>
          <div>
            <dt>Devolucion</dt>
            <dd>{{ alquiler.fechaDevolucion || '-' }}</dd>
          </div>
          <div>
            <dt>Estado</dt>
            <dd>{{ alquiler.estado }}</dd>
          </div>
        </dl>

        <section class="description-panel">
          <h3>Observaciones</h3>
          <p>{{ alquiler.observaciones || 'Sin observaciones registradas.' }}</p>
        </section>
      </section>

      <section class="data-card rental-section">
        <div class="section-heading">
          <h2>Ejemplares</h2>
        </div>
        <div class="table-wrap">
          <table class="app-table app-table--rental-details">
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Libro</th>
                <th>Precio diario</th>
                <th>Dias</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="detalle in alquiler.detalles" :key="detalle.id">
                <td>{{ detalle.ejemplarCodigo }}</td>
                <td>{{ detalle.libroTitulo }}</td>
                <td>{{ formatCurrency(detalle.precioDia) }}</td>
                <td>{{ detalle.cantidadDias }}</td>
                <td>{{ formatCurrency(detalle.subtotal) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="financial-summary financial-summary--cards">
        <div>
          <span>Total alquiler</span>
          <strong>{{ formatCurrency(paymentSummary.totalAlquiler) }}</strong>
        </div>
        <div>
          <span>Total pagado</span>
          <strong>{{ formatCurrency(paymentSummary.totalPagado) }}</strong>
        </div>
        <div>
          <span>Saldo pendiente</span>
          <strong>{{ formatCurrency(paymentSummary.saldoPendiente) }}</strong>
        </div>
      </section>

      <section class="data-card rental-section payments-section">
        <div class="section-heading section-heading--with-action">
          <div>
            <h2>Pagos</h2>
            <p>Historial de pagos registrados para este alquiler.</p>
          </div>
          <div class="payments-actions">
            <AppButton v-if="paymentsErrorMessage" variant="ghost" @click="loadPagos">Reintentar</AppButton>
            <AppButton v-if="!authStore.isDemo && canRegisterPayment" @click="openPaymentDialog">Registrar pago</AppButton>
          </div>
        </div>

        <div v-if="paymentsErrorMessage" class="alert alert--danger payments-alert" role="alert">
          {{ paymentsErrorMessage }}
        </div>

        <div v-if="isPaymentsLoading" class="table-skeleton table-skeleton--compact" aria-label="Cargando pagos">
          <span v-for="row in 3" :key="row" />
        </div>

        <div v-else-if="!paymentsErrorMessage && pagos.length === 0" class="empty-state empty-state--compact">
          <strong>No hay pagos registrados para este alquiler.</strong>
        </div>

        <div v-else-if="!paymentsErrorMessage" class="table-wrap">
          <table class="app-table app-table--payments">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Monto</th>
                <th>Metodo de pago</th>
                <th>Observaciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="pago in pagos" :key="pago.id">
                <td>{{ formatDateTime(pago.fecha) }}</td>
                <td>{{ formatCurrency(pago.monto) }}</td>
                <td>{{ pago.metodoPago }}</td>
                <td>{{ pago.observaciones || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="!authStore.isDemo && alquiler.estado === 'ACTIVO'" class="detail-actions">
        <AppButton variant="ghost" :loading="isActionLoading && pendingAction === 'finalizar'" :disabled="isActionLoading" @click="openAction('finalizar')">
          Finalizar alquiler
        </AppButton>
        <AppButton
          variant="danger"
          :loading="isActionLoading && pendingAction === 'cancelar'"
          :disabled="isActionLoading"
          @click="openAction('cancelar')"
        >
          Cancelar alquiler
        </AppButton>
      </section>

      <ConfirmDialog
        :open="isActionDialogOpen"
        :title="actionCopy.title"
        :message="actionCopy.message"
        :confirm-label="actionCopy.confirmLabel"
        :cancel-label="actionCopy.cancelLabel"
        :destructive="actionCopy.destructive"
        :loading="isActionLoading"
        @cancel="closeActionDialog"
        @confirm="executePendingAction"
      />

      <Teleport to="body">
        <div v-if="isPaymentDialogOpen" class="confirm-backdrop" role="presentation">
          <section class="payment-dialog" role="dialog" aria-modal="true" aria-labelledby="payment-dialog-title">
            <header>
              <p class="eyebrow">Pago</p>
              <h2 id="payment-dialog-title">Registrar pago</h2>
            </header>

            <div class="payment-dialog__summary">
              <div>
                <span>Total alquiler</span>
                <strong>{{ formatCurrency(paymentSummary.totalAlquiler) }}</strong>
              </div>
              <div>
                <span>Total pagado</span>
                <strong>{{ formatCurrency(paymentSummary.totalPagado) }}</strong>
              </div>
              <div>
                <span>Saldo pendiente</span>
                <strong>{{ formatCurrency(paymentSummary.saldoPendiente) }}</strong>
              </div>
            </div>

            <div v-if="paymentErrorMessage" class="alert alert--danger payment-dialog__alert" role="alert">
              {{ paymentErrorMessage }}
            </div>

            <form class="payment-form" @submit.prevent="submitPayment">
              <label class="field" for="payment-amount">
                <span class="field__label">Monto</span>
                <input
                  id="payment-amount"
                  v-model="paymentForm.monto"
                  class="field__control"
                  :class="{ 'field__control--invalid': paymentFieldErrors.monto }"
                  type="text"
                  inputmode="decimal"
                  autocomplete="off"
                  placeholder="0.00"
                />
                <span v-if="paymentFieldErrors.monto" class="field__error">{{ paymentFieldErrors.monto }}</span>
              </label>

              <label class="field" for="payment-method">
                <span class="field__label">Metodo de pago</span>
                <select
                  id="payment-method"
                  v-model="paymentForm.metodoPago"
                  class="field__control"
                  :class="{ 'field__control--invalid': paymentFieldErrors.metodoPago }"
                >
                  <option v-for="method in metodoPagoOptions" :key="method" :value="method">{{ method }}</option>
                </select>
                <span v-if="paymentFieldErrors.metodoPago" class="field__error">{{ paymentFieldErrors.metodoPago }}</span>
              </label>

              <label class="field" for="payment-notes">
                <span class="field__label">Observaciones</span>
                <textarea
                  id="payment-notes"
                  v-model="paymentForm.observaciones"
                  class="field__control field__control--textarea"
                  :class="{ 'field__control--invalid': paymentFieldErrors.observaciones }"
                  maxlength="500"
                  placeholder="Notas del pago..."
                />
                <span v-if="paymentFieldErrors.observaciones" class="field__error">{{ paymentFieldErrors.observaciones }}</span>
              </label>

              <footer class="payment-dialog__actions">
                <AppButton variant="ghost" :disabled="isPaymentSaving" @click="closePaymentDialog">Cancelar</AppButton>
                <AppButton type="submit" :loading="isPaymentSaving" :disabled="isPaymentSaving">Registrar pago</AppButton>
              </footer>
            </form>
          </section>
        </div>
      </Teleport>
    </article>

    <div v-else class="empty-state">
      <strong>No encontramos el alquiler</strong>
      <p>Vuelve al listado para seleccionar otro registro.</p>
      <RouterLink class="button-link button-link--primary" to="/alquileres">Ir a alquileres</RouterLink>
    </div>
  </section>
</template>
