<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/">Easy Deal</RouterLink>
      <nav>
        <RouterLink to="/">交易房间</RouterLink>
      </nav>
    </header>

    <RouterView />
  </div>
</template>
