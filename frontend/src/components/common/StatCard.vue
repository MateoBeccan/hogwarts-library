<script setup lang="ts">
defineProps<{
  icon: string;
  label: string;
  value: string;
  description: string;
}>();
</script>

<template>
  <article class="stat-card">
    <div class="stat-card__icon" aria-hidden="true">{{ icon }}</div>
    <div>
      <p>{{ label }}</p>
      <strong>{{ value }}</strong>
      <span>{{ description }}</span>
    </div>
  </article>
</template>
