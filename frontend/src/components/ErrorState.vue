<script setup lang="ts">
import { computed } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

interface Props {
  title?: string
  description?: string
  error?: string | Error
  showRetry?: boolean
  retryText?: string
  size?: 'small' | 'medium' | 'large'
  type?: 'error' | 'warning' | 'info'
}

const props = withDefaults(defineProps<Props>(), {
  title: '加载失败',
  description: '',
  showRetry: true,
  retryText: '重新加载',
  size: 'medium',
  type: 'error'
})

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const sizeClass = computed(() => `error-${props.size}`)
const typeClass = computed(() => `error-${props.type}`)

const errorMessage = computed(() => {
  if (props.error instanceof Error) {
    return props.error.message
  }
  return props.error || ''
})

const iconColor = computed(() => {
  const map: Record<string, string> = {
    error: '#b34a43',
    warning: '#c58932',
    info: '#2e6b4e'
  }
  return map[props.type] || map.error
})

const iconBgColor = computed(() => {
  const map: Record<string, string> = {
    error: '#f8f0ef',
    warning: '#fff5e6',
    info: '#e8f0ea'
  }
  return map[props.type] || map.error
})

function handleRetry() {
  emit('retry')
}
</script>

<template>
  <div :class="['error-state', sizeClass, typeClass]">
    <div class="error-icon-wrap">
      <slot name="icon">
        <svg class="error-default-icon" viewBox="0 0 120 120" fill="none">
          <circle cx="60" cy="60" r="52" :fill="iconBgColor" />
          <path
            d="M60 36v28"
            :stroke="iconColor"
            stroke-width="5"
            stroke-linecap="round"
          />
          <circle cx="60" cy="78" r="4" :fill="iconColor" />
        </svg>
      </slot>
    </div>
    <p class="error-title">{{ title }}</p>
    <p v-if="description || $slots.description" class="error-desc">
      <slot name="description">{{ description }}</slot>
    </p>
    <p v-if="errorMessage" class="error-detail">
      <span>{{ errorMessage }}</span>
    </p>
    <div v-if="showRetry || $slots.action" class="error-action">
      <slot name="action">
        <el-button type="primary" :icon="Refresh" @click="handleRetry">
          {{ retryText }}
        </el-button>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 24px;
}

.error-icon-wrap {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-default-icon {
  width: 96px;
  height: 96px;
}

.error-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.error-desc {
  margin: 0 0 0;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  max-width: 400px;
}

.error-detail {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--muted);
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 8px 12px;
  max-width: 400px;
  text-align: left;
  word-break: break-all;
}

.error-action {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.error-small {
  padding: 32px 16px;
}

.error-small .error-default-icon {
  width: 64px;
  height: 64px;
}

.error-small .error-title {
  font-size: 14px;
}

.error-small .error-desc {
  font-size: 12px;
}

.error-small .error-action {
  margin-top: 14px;
}

.error-large {
  padding: 72px 32px;
}

.error-large .error-default-icon {
  width: 120px;
  height: 120px;
}

.error-large .error-title {
  font-size: 18px;
}

.error-large .error-desc {
  font-size: 14px;
}

.error-large .error-action {
  margin-top: 24px;
}

@media (max-width: 760px) {
  .error-state {
    padding: 36px 16px;
  }

  .error-default-icon {
    width: 72px;
    height: 72px;
  }

  .error-title {
    font-size: 14px;
  }

  .error-desc {
    font-size: 12px;
  }

  .error-action {
    margin-top: 16px;
  }
}
</style>
