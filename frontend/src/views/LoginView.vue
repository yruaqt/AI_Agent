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
  password: 'admin123'
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
    ElMessage.error('账号或密码错误')
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <div class="login-brand">
        <span class="brand-mark large">榄</span>
        <p>OLIVE ORCHARD INTELLIGENCE</p>
      </div>
      <div class="login-caption">
        <h1>榄园知行</h1>
        <p>学校橄榄实训果园 · 幼果膨大期</p>
        <div class="field-strip">
          <span>5 亩</span>
          <span>300 株</span>
          <span>滴灌</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form">
        <p class="eyebrow">果园作业系统</p>
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

        <div class="demo-accounts">
          <span>管理员 admin / admin123</span>
          <span>学生 student01 / student123</span>
        </div>
      </div>

      <p class="login-footer">榄园知行 · V1.0</p>
    </section>
  </main>
</template>
