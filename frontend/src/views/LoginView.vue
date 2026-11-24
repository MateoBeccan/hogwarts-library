<script setup lang="ts">
import axios from 'axios';
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import loginWallpaper from '@/assets/images/hogwarts-library-login-wallpaper.png';
import { getApiErrorMessage } from '@/api/axios';
import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import { useAuthStore } from '@/stores/auth.store';
import type { ApiError } from '@/types/api';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const demoEnabled = import.meta.env.VITE_DEMO_MODE === 'true';

const form = reactive({
  username: '',
  password: '',
});

const fieldErrors = reactive<Record<string, string>>({});
const generalError = ref('');
const isSubmitting = ref(false);
const isStartingDemo = ref(false);
const retryDemo = ref(false);

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

    await goToLibrary();
  } catch (error) {
    if (axios.isAxiosError<ApiError>(error)) {
      const validationErrors = error.response?.data?.validationErrors ?? {};
      fieldErrors.username = validationErrors.username ?? '';
      fieldErrors.password = validationErrors.password ?? '';
      generalError.value = getApiErrorMessage(error);
      return;
    }

    generalError.value = 'No pudimos iniciar sesion.';
  } finally {
    isSubmitting.value = false;
  }
}

async function goToLibrary(): Promise<void> {
  const redirect = route.query.redirect;
  const destination = typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/dashboard';
  await router.push(destination);
}

async function enterDemo(): Promise<void> {
  if (isSubmitting.value) return;

  generalError.value = '';
  isSubmitting.value = true;
  retryDemo.value = false;
  const slowConnection = window.setTimeout(() => { isStartingDemo.value = true; }, 6000);
  try {
    await authStore.loginDemo();
    await goToLibrary();
  } catch (error) {
    generalError.value = getApiErrorMessage(error);
    retryDemo.value = true;
  } finally {
    window.clearTimeout(slowConnection);
    isStartingDemo.value = false;
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="login-page" :style="{ '--login-wallpaper': `url(${loginWallpaper})` }">
    <section class="login-hero" aria-label="Hogwarts Library">
      <div class="shelf-visual" aria-hidden="true">
        <span v-for="item in 18" :key="item" />
      </div>
      <div class="login-hero__content">
        <p class="eyebrow">Hogwarts Library</p>
        <h1>Library Management System</h1>
        <p>Gestión inteligente de libros, ejemplares, clientes, alquileres y pagos.</p>
      </div>
    </section>

    <section class="login-panel" aria-label="Formulario de acceso">
      <form class="login-card" @submit.prevent="demoEnabled ? enterDemo() : submit()">
        <div>
          <p class="eyebrow">Acceso seguro</p>
          <h2>Bienvenido</h2>
          <p v-if="!demoEnabled">Ingresa tus credenciales para acceder al sistema.</p>
        </div>

        <p v-if="route.query.session === 'expired'" role="status">Tu sesión venció. Volvé a ingresar para continuar.</p>
        <div v-if="generalError" class="alert alert--danger" role="alert">
          {{ generalError }}
        </div>

        <template v-if="!demoEnabled">
          <AppInput
            id="username"
            v-model="form.username"
            label="Usuario"
            placeholder="Ingresa tu usuario"
            autocomplete="username" autofocus
            :error="fieldErrors.username"
          />

          <AppInput
            id="password"
            v-model="form.password"
            label="Contraseña"
            type="password"
            placeholder="Ingresa tu contraseña"
            autocomplete="current-password"
            :error="fieldErrors.password"
          />

          <AppButton type="submit" :loading="isSubmitting" :disabled="!canSubmit">
            Iniciar sesión
          </AppButton>
        </template>

        <section v-else class="login-demo" aria-labelledby="demo-title">
          <h3 id="demo-title">Acceder a la demo</h3>
          <p>Explorá el catálogo, los alquileres y los pagos con datos ficticios. La demo es de solo lectura.</p>
          <AppButton type="submit" :loading="isSubmitting">
            {{ retryDemo ? 'Reintentar acceso' : 'Entrar a la demo' }}
          </AppButton>
          <p v-if="isSubmitting" role="status">
            {{ isStartingDemo ? 'La biblioteca puede tardar hasta un minuto en arrancar.' : 'Conectando con la biblioteca…' }}
          </p>
        </section>
      </form>
    </section>
  </main>
</template>

