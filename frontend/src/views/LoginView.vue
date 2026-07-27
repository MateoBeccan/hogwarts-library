<script setup lang="ts">
import axios from 'axios';
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  username: '',
  password: '',
});

const fieldErrors = reactive<Record<string, string>>({});
const generalError = ref('');
const isSubmitting = ref(false);

const canSubmit = computed(() => form.username.trim().length > 0 && form.password.trim().length > 0);

function validate(): boolean {
  fieldErrors.username = form.username.trim() ? '' : 'El username es obligatorio';
  fieldErrors.password = form.password.trim() ? '' : 'La contrasena es obligatoria';

  return !fieldErrors.username && !fieldErrors.password;
}

async function submit(): Promise<void> {
  generalError.value = '';

  if (!validate()) {
    return;
  }

  try {
    isSubmitting.value = true;
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
    });

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard';
    await router.push(redirect);
  } catch (error) {
    if (axios.isAxiosError<ApiError>(error)) {
      const validationErrors = error.response?.data?.validationErrors ?? {};
      fieldErrors.username = validationErrors.username ?? '';
      fieldErrors.password = validationErrors.password ?? '';
      generalError.value = error.response?.data?.message ?? 'No pudimos iniciar sesion.';
      return;
    }

    generalError.value = 'No pudimos iniciar sesion.';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-hero" aria-label="Hogwarts Library">
      <div class="shelf-visual" aria-hidden="true">
        <span v-for="item in 18" :key="item" />
      </div>
      <div class="login-hero__content">
        <p class="eyebrow">Hogwarts Library</p>
        <h1>Library Management System</h1>
        <p>Gestion inteligente de libros, ejemplares, clientes, alquileres y pagos.</p>
      </div>
    </section>

    <section class="login-panel" aria-label="Formulario de acceso">
      <form class="login-card" @submit.prevent="submit">
        <div>
          <p class="eyebrow">Acceso seguro</p>
          <h2>Bienvenido</h2>
          <p>Ingresa tus credenciales para acceder al sistema.</p>
        </div>

        <div v-if="generalError" class="alert alert--danger" role="alert">
          {{ generalError }}
        </div>

        <AppInput
          id="username"
          v-model="form.username"
          label="Usuario"
          placeholder="admin"
          autocomplete="username"
          :error="fieldErrors.username"
        />

        <AppInput
          id="password"
          v-model="form.password"
          label="Contrasena"
          type="password"
          placeholder="Tu contrasena"
          autocomplete="current-password"
          :error="fieldErrors.password"
        />

        <AppButton type="submit" :loading="isSubmitting" :disabled="!canSubmit">
          Iniciar sesion
        </AppButton>
      </form>
    </section>
  </main>
</template>

