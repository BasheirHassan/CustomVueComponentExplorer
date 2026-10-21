<script setup lang="ts">
defineOptions({
  name: 'AppButton',
})

defineProps<{
  label: string
  primary?: boolean
}>()
</script>

<template>
  <button class="app-button" :class="{ 'app-button--primary': primary }">
    {{ label }}
  </button>
</template>

<style scoped>
.app-button {
  padding: 8px 16px;
  border-radius: 6px;
}
</style>
