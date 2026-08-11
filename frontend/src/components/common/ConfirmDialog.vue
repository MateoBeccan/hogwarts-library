<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';

import AppButton from '@/components/common/AppButton.vue';

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    message: string;
    confirmLabel: string;
    cancelLabel?: string;
    loading?: boolean;
    destructive?: boolean;
  }>(),
  {
    cancelLabel: 'Cancelar',
    loading: false,
    destructive: false,
  },
);

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && props.open && !props.loading) {
    emit('cancel');
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) window.addEventListener('keydown', handleKeydown);
    else window.removeEventListener('keydown', handleKeydown);
  },
);

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="confirm-backdrop" role="presentation">
      <section class="confirm-dialog" role="dialog" aria-modal="true" :aria-labelledby="`${title}-title`">
        <div>
          <p class="eyebrow">Confirmacion</p>
          <h2 :id="`${title}-title`">{{ title }}</h2>
          <p>{{ message }}</p>
        </div>

        <footer class="confirm-dialog__actions">
          <AppButton variant="ghost" :disabled="loading" @click="$emit('cancel')">{{ cancelLabel }}</AppButton>
          <AppButton
            :variant="destructive ? 'danger' : 'primary'"
            :loading="loading"
            :disabled="loading"
            @click="$emit('confirm')"
          >
            {{ confirmLabel }}
          </AppButton>
        </footer>
      </section>
    </div>
  </Teleport>
</template>