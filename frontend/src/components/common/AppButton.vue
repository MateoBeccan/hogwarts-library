<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'button' | 'submit' | 'reset';
    variant?: 'primary' | 'ghost' | 'subtle';
    disabled?: boolean;
    loading?: boolean;
  }>(),
  {
    type: 'button',
    variant: 'primary',
    disabled: false,
    loading: false,
  },
);
</script>

<template>
  <button class="app-button" :class="`app-button--${variant}`" :type="type" :disabled="disabled || loading">
    <span v-if="loading" class="button-spinner" aria-hidden="true" />
    <slot />
  </button>
</template>
