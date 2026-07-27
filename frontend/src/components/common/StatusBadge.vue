<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'success' | 'warning' | 'danger' | 'info';
  }>(),
  {
    tone: 'info',
  },
);
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <slot />
  </span>
</template>
