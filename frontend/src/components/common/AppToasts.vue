<script setup lang="ts">
import { useToastStore } from '@/stores/toast.store';

const toastStore = useToastStore();
</script>

<template>
  <Teleport to="body">
    <div class="toast-region" aria-live="polite" aria-label="Mensajes del sistema">
      <article v-for="toast in toastStore.messages" :key="toast.id" class="toast" :class="`toast--${toast.tone}`">
        <span>{{ toast.message }}</span>
        <button type="button" aria-label="Cerrar mensaje" @click="toastStore.dismiss(toast.id)">x</button>
      </article>
    </div>
  </Teleport>
</template>