<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import api, { unwrap } from '@/api'
import { Operation, Bowl, Coin, MagicStick, TrendCharts } from '@element-plus/icons-vue'

const active = ref('irrigation')
const loading = ref(false)
const result = ref<any>(null)

const forms = reactive({
  irrigation: {
    treeCount: 300,
    litersPerTree: 20
  },
  fertilizer: {
    treeCount: 300,
    amountPerTree: 12,
    unit: 'KG'
  },
  dilution: {
    solutionLiters: 200,
    dilutionRatio: 1000
  },
  yieldEstimate: {
    sampleTreeCount: 20,
    sampleYieldKg: 360,
    totalTreeCount: 300
  }
})

interface CalculatorConfig {
  title: string
  endpoint: string
  icon: any
  description: string
}

const configs: Record<string, CalculatorConfig> = {
  irrigation: {
    title: '灌溉量计算',
    endpoint: '/calculators/irrigation',
    icon: Bowl,
    description: '按株数与单株用水量计算全园用水量'
  },
  fertilizer: {
    title: '肥料量计算',
    endpoint: '/calculators/fertilizer',
    icon: Coin,
    description: '按株数和单株用量计算肥料总量'
  },
  dilution: {
    title: '药剂稀释计算',
    endpoint: '/calculators/dilution',
    icon: MagicStick,
    description: '根据目标药液体积和稀释倍数计算原药用量'
  },
  yieldEstimate: {
    title: '产量估算',
    endpoint: '/calculators/yield-estimate',
    icon: TrendCharts,
    description: '根据抽样数据估算果园总产量'
  }
}

const currentConfig = computed(() => configs[active.value])

async function calculate() {
  loading.value = true
  result.value = null
  try {
    const formData = (forms as any)[active.value]
    const res = unwrap<any>(await api.post(currentConfig.value.endpoint, formData))
    result.value = res
  } finally {
    loading.value = false
  }
}

function switchTab(key: string) {
  active.value = key
  result.value = null
}

const resultItems = computed(() => {
  if (!result.value) return []
  const r = result.value
  const items: Array<{ label: string; value: string; unit?: string }> = []

  switch (active.value) {
    case 'irrigation':
      items.push({ label: '总用水量', value: r.totalLiters?.toLocaleString() || '0', unit: '升' })
      items.push({ label: '总用水量', value: r.totalCubicMeters?.toLocaleString() || '0', unit: '立方米' })
      break
    case 'fertilizer':
      items.push({ label: '肥料总量', value: r.totalKg?.toLocaleString() || '0', unit: '千克' })
      items.push({ label: '肥料总量', value: r.totalTon?.toLocaleString() || '0', unit: '吨' })
      break
    case 'dilution':
      items.push({ label: '原药用量', value: r.originalAgentMilliliters?.toLocaleString() || '0', unit: '毫升' })
      break
    case 'yieldEstimate':
      items.push({ label: '平均单株产量', value: r.averageYieldPerTreeKg?.toLocaleString() || '0', unit: '千克/株' })
      items.push({ label: '估算总产量', value: r.estimatedTotalYieldKg?.toLocaleString() || '0', unit: '千克' })
      items.push({ label: '估算总产量', value: r.estimatedTotalYieldTon?.toLocaleString() || '0', unit: '吨' })
      break
  }
  return items
})

const hasWarning = computed(() => {
  return result.value && result.value.warning
})
</script>

<template>
  <div>
    <div class="page-title-row">
      <div>
        <h2>农业用量计算</h2>
        <p>统一使用后端 BigDecimal 计算服务，确保计算精度</p>
      </div>
      <div class="toolbar">
        <span class="status-pill">精确计算</span>
      </div>
    </div>

    <div class="calculator-layout">
      <aside class="calc-tabs">
        <button
          v-for="(config, key) in configs"
          :key="key"
          :class="{ active: key === active }"
          @click="switchTab(key)"
        >
          <el-icon :size="20">
            <component :is="config.icon" />
          </el-icon>
          <span>{{ config.title }}</span>
        </button>
      </aside>

      <section class="panel calc-panel">
        <header class="panel-header">
          <div class="calc-header-title">
            <el-icon :size="18" :color="'var(--green)'">
              <component :is="currentConfig.icon" />
            </el-icon>
            <h3>{{ currentConfig.title }}</h3>
          </div>
          <span class="status-pill">精确计算</span>
        </header>

        <div class="calc-form">
          <p class="calc-desc">{{ currentConfig.description }}</p>

          <el-form v-if="active === 'irrigation'" label-position="top">
            <el-form-item label="果园株数">
              <el-input-number
                v-model="forms.irrigation.treeCount"
                :min="1"
                :step="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="单株用水量（升）">
              <el-input-number
                v-model="forms.irrigation.litersPerTree"
                :min="0.01"
                :step="1"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>

          <el-form v-if="active === 'fertilizer'" label-position="top">
            <el-form-item label="果园株数">
              <el-input-number
                v-model="forms.fertilizer.treeCount"
                :min="1"
                :step="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="单株用量">
              <el-input-number
                v-model="forms.fertilizer.amountPerTree"
                :min="0.01"
                :step="0.5"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="单位">
              <el-segmented
                v-model="forms.fertilizer.unit"
                :options="[
                  { label: '克 (G)', value: 'G' },
                  { label: '千克 (KG)', value: 'KG' }
                ]"
                block
              />
            </el-form-item>
          </el-form>

          <el-form v-if="active === 'dilution'" label-position="top">
            <el-form-item label="目标药液体积（升）">
              <el-input-number
                v-model="forms.dilution.solutionLiters"
                :min="0.01"
                :step="10"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="稀释倍数">
              <el-input-number
                v-model="forms.dilution.dilutionRatio"
                :min="2"
                :step="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>

          <el-form v-if="active === 'yieldEstimate'" label-position="top">
            <el-form-item label="抽样株数">
              <el-input-number
                v-model="forms.yieldEstimate.sampleTreeCount"
                :min="1"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="抽样总产量（千克）">
              <el-input-number
                v-model="forms.yieldEstimate.sampleYieldKg"
                :min="0.01"
                :step="1"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="果园总株数">
              <el-input-number
                v-model="forms.yieldEstimate.totalTreeCount"
                :min="1"
                :step="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>

          <el-button
            type="primary"
            :icon="Operation"
            :loading="loading"
            @click="calculate"
            class="calc-btn"
          >
            开始计算
          </el-button>
        </div>
      </section>

      <section class="result-area">
        <div v-if="!result" class="result-empty">
          <el-icon :size="48"><Operation /></el-icon>
          <span class="empty-title">等待计算</span>
          <span class="empty-desc">填写参数后点击计算按钮查看结果</span>
        </div>

        <template v-else>
          <p class="eyebrow">计算结果</p>
          <h3 class="result-formula" v-if="result.formula">
            公式：{{ result.formula }}
          </h3>

          <div class="result-grid">
            <div v-for="(item, index) in resultItems" :key="index" class="result-item">
              <span class="result-label">{{ item.label }}</span>
              <div class="result-value-wrap">
                <strong class="result-value">{{ item.value }}</strong>
                <span class="result-unit" v-if="item.unit">{{ item.unit }}</span>
              </div>
            </div>
          </div>

          <el-alert
            v-if="hasWarning"
            :title="result.warning"
            type="warning"
            :closable="false"
            show-icon
            class="result-warning"
          />

          <el-alert
            v-if="active === 'dilution'"
            title="安全提示：药剂稀释计算仅供参考，实际使用前请核对产品标签、登记作物、安全间隔期并由指导教师确认。"
            type="error"
            :closable="false"
            show-icon
            class="result-warning"
          />

          <el-alert
            v-if="active === 'yieldEstimate'"
            title="提示：该结果为抽样估算值，不作为最终产量数据。"
            type="info"
            :closable="false"
            show-icon
            class="result-warning"
          />
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.calculator-layout {
  display: grid;
  grid-template-columns: 180px minmax(320px, 0.75fr) minmax(360px, 1fr);
  gap: 16px;
  align-items: stretch;
}

.calc-tabs {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.calc-tabs button {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  text-align: left;
  padding: 14px 16px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s ease;
  font-size: 13px;
}

.calc-tabs button:hover {
  border-color: var(--green);
  color: var(--green);
  background: var(--green-light);
}

.calc-tabs button.active {
  color: var(--green);
  border-color: var(--green);
  font-weight: 600;
  background: var(--green-light);
  box-shadow: inset 3px 0 var(--green);
}

.calc-panel {
  display: flex;
  flex-direction: column;
}

.calc-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.calc-header-title h3 {
  margin: 0;
  font-size: 15px;
}

.calc-form {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.calc-desc {
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.6;
}

.calc-form .el-form-item {
  margin-bottom: 16px;
}

.calc-btn {
  margin-top: 8px;
  width: 100%;
  height: 42px;
  font-size: 14px;
}

.result-area {
  background: linear-gradient(145deg, #26342e 0%, #1f2d27 100%);
  color: white;
  border-radius: 8px;
  padding: 28px 26px;
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.result-area .eyebrow {
  color: #b6cf82;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin: 0 0 8px;
}

.result-formula {
  font-size: 14px;
  font-weight: 500;
  color: #dfe8e2;
  margin: 0 0 24px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-family: 'Consolas', 'Monaco', monospace;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-bottom: 22px;
}

.result-item {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.result-label {
  font-size: 11px;
  color: #a9b9b0;
  font-weight: 500;
}

.result-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.result-value {
  font-size: 24px;
  font-weight: 700;
  color: #e8f0ea;
  font-family: 'Consolas', 'Monaco', monospace;
}

.result-unit {
  font-size: 12px;
  color: #8fa097;
  font-weight: 500;
}

.result-warning {
  margin-top: auto;
}

.result-warning .el-alert {
  border-radius: 6px;
  border: none;
}

.result-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #8fa097;
  flex: 1;
}

.result-empty .el-icon {
  opacity: 0.6;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #b0c0b6;
}

.empty-desc {
  font-size: 12px;
  color: #7a8c81;
}

@media (max-width: 1000px) {
  .calculator-layout {
    grid-template-columns: 160px 1fr;
  }
  .result-area {
    grid-column: 1 / -1;
    min-height: 280px;
  }
  .result-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .calculator-layout {
    grid-template-columns: 1fr;
  }
  .calc-tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .result-area {
    grid-column: auto;
    min-height: 320px;
  }
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
