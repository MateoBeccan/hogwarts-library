<script setup lang="ts">
defineProps<{
  icon: string;
  iconImage?: string;
  label: string;
  value: string;
  description: string;
}>();
</script>

<template>
  <article class="stat-card">
    <div class="stat-card__icon" aria-hidden="true">
      <img v-if="iconImage" class="stat-card__image" :src="iconImage" alt="" />
      <span v-else>{{ icon }}</span>
    </div>
    <div>
      <p>{{ label }}</p>
      <strong>{{ value }}</strong>
      <span>{{ description }}</span>
    </div>
  </article>
</template>