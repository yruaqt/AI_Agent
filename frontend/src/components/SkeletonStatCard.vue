<script setup lang="ts">
defineProps<{
  count?: number
}>()
</script>

<template>
  <div class="stat-grid skeleton-grid">
    <div v-for="i in (count || 4)" :key="i" class="stat-card skeleton-card">
      <div class="skeleton-line skeleton-line--short"></div>
      <div class="skeleton-line skeleton-line--large"></div>
      <div class="skeleton-line skeleton-line--medium"></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.skeleton-card {
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 18px;
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
}

.skeleton-line {
  height: 12px;
  background: linear-gradient(
    90deg,
    #e8ecea 25%,
    #f0f3f1 50%,
    #e8ecea 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  border-radius: 4px;
}

.skeleton-line--short {
  width: 40%;
  height: 12px;
}

.skeleton-line--medium {
  width: 60%;
  height: 11px;
}

.skeleton-line--large {
  width: 80%;
  height: 26px;
  border-radius: 6px;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 1200px) {
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .skeleton-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .skeleton-card {
    padding: 12px;
    min-height: 84px;
    gap: 8px;
  }
  .skeleton-line--large {
    height: 19px;
  }
}
</style>
