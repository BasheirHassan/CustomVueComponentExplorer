<script setup lang="ts">
import AppButton from './components/AppButton.vue'
import UserCard from './components/UserCard.vue'
import LegacyWidget from './components/LegacyWidget.vue'
</script>

<template>
  <main>
    <h1>Demo</h1>

    <AppButton label="Primary" primary />
    <app-button label="Secondary" />

    <UserCard user-name="Sara">
      <LegacyWidget />
    </UserCard>

    <app-button label="Third" />
  </main>
</template>
