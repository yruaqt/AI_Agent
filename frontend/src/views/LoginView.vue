<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
})

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')

    // 检查是否有保存的跳转路径
    const redirectPath = sessionStorage.getItem('redirectAfterLogin')
    if (redirectPath) {
      sessionStorage.removeItem('redirectAfterLogin')
      router.push(redirectPath)
    } else {
      router.push('/')
    }
  } catch {
    // 具体错误已由全局 Axios 拦截器提示，避免锁定提示与“账号或密码错误”重复弹出。
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <div class="login-caption">
        <h1>榄园知行</h1>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form">
        <h2>账户登录</h2>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @submit.prevent="submit"
          status-icon
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="用户名"
              :prefix-icon="User"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="密码"
              :prefix-icon="Lock"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            :loading="auth.loading"
            native-type="submit"
            @click="submit"
          >
            进入系统
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </el-form>
      </div>

      <p class="login-footer">榄园知行 · V1.0</p>
    </section>
  </main>
</template>
