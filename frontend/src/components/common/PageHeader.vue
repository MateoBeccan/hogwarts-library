<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
}>();
</script>

<template>
  <header class="page-header">
    <div>
      <p v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</p>
      <h1>{{ title }}</h1>
    </div>
    <slot />
  </header>
</template>
