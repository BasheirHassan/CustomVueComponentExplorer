<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'UserCard',
  props: {
    userName: { type: String, required: true },
  },
})
</script>

<template>
  <div class="user-card">
    <h3>{{ userName }}</h3>
    <slot />
  </div>
</template>
