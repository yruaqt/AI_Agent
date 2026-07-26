<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  description?: string
  icon?: any
  actionText?: string
  showAction?: boolean
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  title: '暂无数据',
  description: '',
  actionText: '去添加',
  showAction: false,
  size: 'medium'
})

const emit = defineEmits<{
  (e: 'action'): void
}>()

const sizeClass = computed(() => `empty-${props.size}`)

function handleAction() {
  emit('action')
}
</script>

<template>
  <div :class="['empty-state', sizeClass]">
    <div class="empty-icon-wrap">
      <slot name="icon">
        <component v-if="icon" :is="icon" class="empty-icon" />
        <svg v-else class="empty-default-icon" viewBox="0 0 120 120" fill="none">
          <circle cx="60" cy="60" r="52" fill="#e8f0ea" />
          <path
            d="M40 55l12 12 28-28"
            stroke="#9fbd63"
            stroke-width="5"
            stroke-linecap="round"
            stroke-linejoin="round"
            fill="none"
            opacity="0.5"
          />
          <circle cx="60" cy="60" r="4" fill="#9fbd63" opacity="0.3" />
        </svg>
      </slot>
    </div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="description || $slots.description" class="empty-desc">
      <slot name="description">{{ description }}</slot>
    </p>
    <div v-if="showAction || $slots.action" class="empty-action">
      <slot name="action">
        <el-button type="primary" @click="handleAction">{{ actionText }}</el-button>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--muted);
  padding: 48px 24px;
}

.empty-icon-wrap {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--line);
}

.empty-default-icon {
  width: 96px;
  height: 96px;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--ink);
}

.empty-desc {
  margin: 0 0 0;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  max-width: 360px;
}

.empty-action {
  margin-top: 20px;
}

.empty-small {
  padding: 32px 16px;
}

.empty-small .empty-default-icon {
  width: 64px;
  height: 64px;
}

.empty-small .empty-icon {
  font-size: 36px;
}

.empty-small .empty-title {
  font-size: 14px;
}

.empty-small .empty-desc {
  font-size: 12px;
}

.empty-small .empty-action {
  margin-top: 14px;
}

.empty-large {
  padding: 72px 32px;
}

.empty-large .empty-default-icon {
  width: 120px;
  height: 120px;
}

.empty-large .empty-icon {
  font-size: 64px;
}

.empty-large .empty-title {
  font-size: 18px;
}

.empty-large .empty-desc {
  font-size: 14px;
}

.empty-large .empty-action {
  margin-top: 24px;
}

@media (max-width: 760px) {
  .empty-state {
    padding: 36px 16px;
  }

  .empty-default-icon {
    width: 72px;
    height: 72px;
  }

  .empty-icon {
    font-size: 36px;
  }

  .empty-title {
    font-size: 14px;
  }

  .empty-desc {
    font-size: 12px;
  }

  .empty-action {
    margin-top: 16px;
  }
}
</style>
