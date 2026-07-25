<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="forbidden-page">
    <div class="forbidden-content">
      <el-icon class="forbidden-icon" :size="80">
        <Lock />
      </el-icon>

      <h1>403</h1>
      <h2>访问受限</h2>
      <p class="forbidden-message">抱歉，您没有权限访问此页面。如需帮助，请联系管理员。</p>

      <div class="forbidden-actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7f6 0%, #e8f0ea 100%);
  padding: 20px;
}

.forbidden-content {
  text-align: center;
  max-width: 400px;
  background: white;
  padding: 48px 32px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.forbidden-icon {
  color: var(--amber, #c58932);
  margin-bottom: 24px;
}

.forbidden-content h1 {
  font-size: 72px;
  margin: 0;
  color: var(--green, #2e6b4e);
  font-weight: 300;
  line-height: 1;
}

.forbidden-content h2 {
  font-size: 24px;
  margin: 12px 0 16px;
  color: var(--ink, #202824);
}

.forbidden-message {
  color: var(--muted, #6d7872);
  font-size: 14px;
  margin-bottom: 32px;
  line-height: 1.6;
}

.forbidden-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.forbidden-actions .el-button {
  min-width: 110px;
}
</style>