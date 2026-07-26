<script setup lang="ts">
defineProps<{
  type?: 'session' | 'message'
}>()
</script>

<template>
  <div v-if="type === 'session'" class="skeleton-session-list">
    <div v-for="i in 6" :key="i" class="skeleton-session-item">
      <div class="skeleton-line skeleton-line--title"></div>
      <div class="skeleton-line skeleton-line--tiny"></div>
    </div>
  </div>
  <div v-else class="skeleton-messages">
    <div class="skeleton-message assistant">
      <div class="skeleton-avatar"></div>
      <div class="skeleton-message-body">
        <div class="skeleton-line skeleton-line--tiny"></div>
        <div class="skeleton-line skeleton-line--long"></div>
        <div class="skeleton-line skeleton-line--medium"></div>
        <div class="skeleton-line skeleton-line--short"></div>
      </div>
    </div>
    <div class="skeleton-message user">
      <div class="skeleton-avatar user"></div>
      <div class="skeleton-message-body">
        <div class="skeleton-line skeleton-line--tiny"></div>
        <div class="skeleton-line skeleton-line--medium"></div>
      </div>
    </div>
    <div class="skeleton-message assistant">
      <div class="skeleton-avatar"></div>
      <div class="skeleton-message-body">
        <div class="skeleton-line skeleton-line--tiny"></div>
        <div class="skeleton-line skeleton-line--long"></div>
        <div class="skeleton-line skeleton-line--long"></div>
        <div class="skeleton-line skeleton-line--medium"></div>
        <div class="skeleton-tool-chips">
          <div class="skeleton-chip"></div>
          <div class="skeleton-chip"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
}

.skeleton-session-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 3px 6px;
  padding: 11px;
  border-radius: 5px;
}

.skeleton-messages {
  padding: 20px 8%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.skeleton-message {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
}

.skeleton-message.user {
  direction: rtl;
}

.skeleton-message.user .skeleton-message-body {
  direction: ltr;
  text-align: right;
}

.skeleton-avatar {
  width: 32px;
  height: 32px;
  border-radius: 5px;
  background: linear-gradient(
    90deg,
    #e8ecea 25%,
    #f0f3f1 50%,
    #e8ecea 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-message-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  width: 60px;
  height: 10px;
}

.skeleton-line--short {
  width: 35%;
}

.skeleton-line--medium {
  width: 60%;
}

.skeleton-line--long {
  width: 100%;
}

.skeleton-line--title {
  width: 80%;
  height: 12px;
}

.skeleton-tool-chips {
  display: flex;
  gap: 7px;
  margin-top: 4px;
}

.skeleton-chip {
  width: 80px;
  height: 22px;
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

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 800px) {
  .skeleton-messages {
    padding: 16px;
  }
}
</style>
