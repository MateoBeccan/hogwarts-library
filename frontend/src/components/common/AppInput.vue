<script setup lang="ts">
defineProps<{
  id: string;
  label: string;
  modelValue: string;
  type?: string;
  placeholder?: string;
  error?: string;
  autocomplete?: string;
}>();

defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<template>
  <label class="field" :for="id">
    <span class="field__label">{{ label }}</span>
    <input
      :id="id"
      class="field__control"
      :class="{ 'field__control--invalid': error }"
      :type="type ?? 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <span v-if="error" class="field__error">{{ error }}</span>
  </label>
</template>
