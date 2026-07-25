<script setup lang="ts">
defineProps<{
  type?: 'list' | 'card' | 'weather'
}>()
</script>

<template>
  <div class="skeleton-panel">
    <div class="skeleton-panel-header">
      <div class="skeleton-line skeleton-line--short"></div>
      <div class="skeleton-line skeleton-line--tiny"></div>
    </div>
    <div class="skeleton-panel-body">
      <template v-if="type === 'list'">
        <div v-for="i in 5" :key="i" class="skeleton-list-item">
          <div class="skeleton-bar"></div>
          <div class="skeleton-list-content">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line skeleton-line--long"></div>
          </div>
          <div class="skeleton-list-meta">
            <div class="skeleton-line skeleton-line--tiny"></div>
            <div class="skeleton-tag"></div>
          </div>
        </div>
      </template>
      <template v-else-if="type === 'weather'">
        <div v-for="i in 3" :key="i" class="skeleton-weather-item">
          <div>
            <div class="skeleton-line skeleton-line--tiny"></div>
            <div class="skeleton-line skeleton-line--small"></div>
          </div>
          <div>
            <div class="skeleton-line skeleton-line--medium"></div>
          </div>
        </div>
      </template>
      <template v-else>
        <div v-for="i in 4" :key="i" class="skeleton-card-item">
          <div class="skeleton-line skeleton-line--title"></div>
          <div class="skeleton-line skeleton-line--long"></div>
          <div class="skeleton-line skeleton-line--medium"></div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.skeleton-panel {
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
}

.skeleton-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}

.skeleton-panel-body {
  padding: 7px 18px 18px;
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

.skeleton-line--tiny {
  width: 50px;
  height: 10px;
}

.skeleton-line--small {
  width: 70px;
  height: 12px;
}

.skeleton-line--short {
  width: 80px;
  height: 15px;
}

.skeleton-line--title {
  width: 140px;
  height: 14px;
  margin-bottom: 6px;
}

.skeleton-line--medium {
  width: 50%;
  height: 12px;
}

.skeleton-line--long {
  width: 100%;
  height: 12px;
  margin-bottom: 4px;
}

.skeleton-tag {
  width: 48px;
  height: 20px;
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

.skeleton-list-item {
  display: grid;
  grid-template-columns: 4px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.skeleton-list-item:last-child {
  border-bottom: none;
}

.skeleton-bar {
  height: 34px;
  border-radius: 2px;
  background: linear-gradient(
    90deg,
    #e8ecea 25%,
    #f0f3f1 50%,
    #e8ecea 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-list-content {
  min-width: 0;
}

.skeleton-list-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.skeleton-weather-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 2px;
  border-bottom: 1px solid var(--line);
}

.skeleton-weather-item:last-child {
  border-bottom: none;
}

.skeleton-weather-item > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skeleton-card-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skeleton-card-item:last-child {
  border-bottom: none;
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
  .skeleton-panel-header {
    padding: 14px;
  }
  .skeleton-panel-body {
    padding: 6px 14px 14px;
  }
  .skeleton-list-item {
    gap: 10px;
    padding: 14px 0;
  }
  .skeleton-bar {
    height: 30px;
  }
}
</style>
