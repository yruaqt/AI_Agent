<script setup lang="ts">
defineProps<{
  rows?: number
  columns?: number
}>()

function getColumnWidth(col: number, total: number): string {
  const widths: Record<number, string> = {
    1: '18%',
    2: '10%',
    3: '8%',
    4: '15%',
    5: '12%',
    6: '12%',
    7: '10%',
    8: '15%'
  }
  return widths[col] || `${100 / total}%`
}
</script>

<template>
  <div class="skeleton-table">
    <div class="skeleton-table-header">
      <div
        v-for="i in (columns || 6)"
        :key="'h-' + i"
        class="skeleton-cell skeleton-cell--header"
      ></div>
    </div>
    <div v-for="r in (rows || 5)" :key="'r-' + r" class="skeleton-table-row">
      <div
        v-for="c in (columns || 6)"
        :key="'c-' + r + '-' + c"
        class="skeleton-cell"
        :style="{ width: getColumnWidth(c, columns || 6) }"
      ></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-table {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}

.skeleton-table-header {
  display: flex;
  background: var(--green-light);
  padding: 14px 16px;
  gap: 16px;
  border-bottom: 2px solid var(--line);
}

.skeleton-table-row {
  display: flex;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  gap: 16px;
  background: white;
}

.skeleton-cell {
  height: 14px;
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

.skeleton-cell--header {
  height: 12px;
  opacity: 0.6;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 760px) {
  .skeleton-table-header,
  .skeleton-table-row {
    padding: 10px 12px;
    gap: 10px;
  }
  .skeleton-cell {
    height: 12px;
  }
}
</style>
