<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import api,{unwrap} from '@/api'
import type { Orchard,PageData } from '@/types'
import { Refresh, ArrowRight, Cherry } from '@element-plus/icons-vue'
import SkeletonStatCard from '@/components/SkeletonStatCard.vue'
import SkeletonPanel from '@/components/SkeletonPanel.vue'
import ErrorState from '@/components/ErrorState.vue'
import EmptyState from '@/components/EmptyState.vue'

const orchard=ref<Orchard|null>(null)
const weather=ref<any>(null)
const tasks=ref<any[]>([])
const loading=ref(false)
const error=ref<string | null>(null)

const done=computed(()=>tasks.value.filter(t=>t.status==='DONE').length)

async function load(){
  loading.value=true
  error.value=null
  try{
    const o=unwrap<PageData<Orchard>>(await api.get('/orchards'))
    orchard.value=o.items[0]||null
    if(orchard.value){
      const [w,t]=await Promise.all([
        api.get(`/orchards/${orchard.value.id}/weather?days=3`).catch(() => ({ data: { data: null } })),
        api.get(`/tasks?orchardId=${orchard.value.id}&pageSize=8`).catch(() => ({ data: { data: { items: [], total: 0 } } }))
      ])
      weather.value=unwrap<any>(w)
      tasks.value=unwrap<PageData<any>>(t).items
    }
  }catch(e: any){
    error.value=e?.message || '加载数据失败'
    console.error('加载首页数据失败', e)
  }finally{
    loading.value=false
  }
}

onMounted(load)

const phenologyNames:Record<string,string>={
  FRUIT_EXPANSION:'幼果膨大期',
  FLOWERING:'开花期',
  FRUIT_SET:'坐果期',
  MATURITY:'成熟期',
  HARVEST:'采收期'
}
</script>

<template>
  <div>
    <div class="page-title-row">
      <div>
        <h2>{{ loading ? '数据加载中' : (orchard?.name || '果园数据') }}</h2>
        <p v-if="!loading && orchard">{{ orchard.region }} · 当前物候期 {{ phenologyNames[orchard.currentPhenology||'']||orchard.currentPhenology }}</p>
        <p v-else-if="!loading" class="muted">暂无果园信息</p>
        <p v-else class="muted">正在获取果园信息…</p>
      </div>
      <div class="toolbar">
        <span class="status-pill" v-if="!loading && orchard">档案已确认</span>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <template v-if="loading">
      <SkeletonStatCard :count="4" />
      <div class="content-grid">
        <SkeletonPanel type="list" />
        <SkeletonPanel type="weather" />
      </div>
    </template>

    <template v-else-if="error">
      <div class="panel">
        <ErrorState
          :title=" '数据加载失败' "
          :description=" '果园信息、天气或任务数据加载失败，请检查网络连接后重试' "
          :error="error"
          @retry="load"
        />
      </div>
    </template>

    <template v-else-if="!orchard">
      <div class="panel">
        <EmptyState
          icon="Cherry"
          title="暂无果园档案"
          description="系统中还没有创建任何果园，请联系管理员创建果园档案后再查看首页信息。"
          :show-action="false"
        />
      </div>
    </template>

    <template v-else>
      <div class="stat-grid">
        <div class="stat-card">
          <span class="label">果园面积</span>
          <strong>{{ orchard.areaMu||'--' }}<small> 亩</small></strong>
          <span class="trend">露地栽培</span>
        </div>
        <div class="stat-card">
          <span class="label">橄榄树</span>
          <strong>{{ orchard.treeCount||'--' }}<small> 株</small></strong>
          <span class="trend">{{ orchard.treeAgeYears||'--' }} 年生</span>
        </div>
        <div class="stat-card">
          <span class="label">今日任务</span>
          <strong>{{ tasks.length }}<small> 项</small></strong>
          <span class="trend">已完成 {{ done }} 项</span>
        </div>
        <div class="stat-card">
          <span class="label">天气数据</span>
          <strong style="font-size:20px">{{ weather?.forecast?.[0]?.dayWeather || weather?.forecasts?.[0]?.phenomenon || '--' }}</strong>
          <span :class="['trend', weather?.demoData?'priority-medium':'']">
            {{ weather?.source || weather?.provider || '等待查询' }}
            <span v-if="weather?.cached" class="cached-badge">（缓存）</span>
          </span>
        </div>
      </div>

      <div class="content-grid">
        <section class="panel">
          <header class="panel-header">
            <h3>今日农事</h3>
            <router-link to="/tasks" class="text-link">
              任务管理 <el-icon><ArrowRight/></el-icon>
            </router-link>
          </header>
          <div class="task-list">
            <div v-if="!tasks.length" class="task-empty">
              <EmptyState
                size="small"
                title="暂无今日任务"
                description="前往任务管理页面，使用 Agent 生成今日农事任务。"
                :show-action="true"
                action-text="生成任务"
                @action="$router.push('/tasks')"
              />
            </div>
            <div
              v-for="task in tasks.slice(0,5)"
              :key="task.id"
              class="task-row"
            >
              <span :class="`priority-bar ${task.priority.toLowerCase()}`"></span>
              <div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.content }}</p>
              </div>
              <div class="task-meta">
                <span>{{ task.suggestedTime }}</span>
                <el-tag size="small" effect="plain">{{ task.status }}</el-tag>
              </div>
            </div>
          </div>
        </section>

        <aside class="panel">
          <header class="panel-header">
            <h3>未来天气</h3>
            <span v-if="weather?.demoData" class="status-pill amber">演示数据</span>
            <span v-if="weather?.cached" class="status-pill">缓存数据</span>
          </header>
          <div class="weather-list">
            <div v-if="!weather || (!weather.forecast && !weather.forecasts)" class="weather-empty">
              <EmptyState
                size="small"
                title="暂无天气数据"
                description="天气服务暂时不可用或尚未查询。"
                :show-retry="true"
                retry-text="刷新重试"
                @retry="load"
              />
            </div>
            <div
              v-for="day in (weather.forecast || weather.forecasts || [])"
              :key="day.date"
              class="weather-day"
            >
              <div>
                <strong>{{ day.date?.slice(5) || day.dateStr }}</strong>
                <span>{{ day.dayWeather || day.phenomenon }}</span>
              </div>
              <div>
                <b>{{ day.maxTemperatureC || day.maxTemperature }}°</b>
                <small>/ {{ day.minTemperatureC || day.minTemperature }}°</small>
              </div>
            </div>
            <el-alert
              v-if="weather?.warning"
              :title="weather.warning"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.stat-card small {
  font-size: 13px;
  font-weight: 500;
}

.text-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--green);
  font-size: 12px;
  text-decoration: none;
}

.task-list {
  padding: 0 18px;
}

.task-empty {
  padding: 8px 0;
}

.task-row {
  display: grid;
  grid-template-columns: 4px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.task-row:last-child {
  border: 0;
}

.task-row strong {
  font-size: 13px;
}

.task-row p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 640px;
}

.priority-bar {
  height: 34px;
  border-radius: 2px;
  background: var(--green);
}

.priority-bar.high {
  background: var(--red);
}

.priority-bar.medium {
  background: var(--amber);
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 11px;
}

.weather-list {
  padding: 7px 18px 18px;
}

.weather-empty {
  padding: 8px 0;
}

.weather-day {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 2px;
  border-bottom: 1px solid var(--line);
}

.weather-day > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.weather-day span,
.weather-day small {
  color: var(--muted);
  font-size: 11px;
}

.weather-day b {
  font-size: 20px;
}

.weather-list .el-alert {
  margin-top: 14px;
}

.cached-badge {
  font-size: 10px;
  opacity: 0.8;
}

.empty-inline {
  padding: 30px;
  color: var(--muted);
  text-align: center;
  font-size: 13px;
}

@media (max-width: 760px) {
  .task-list {
    padding: 0 14px;
  }

  .task-row {
    gap: 10px;
    padding: 14px 0;
  }

  .task-row strong {
    font-size: 12px;
  }

  .task-row p {
    font-size: 11px;
    max-width: none;
    -webkit-line-clamp: 2;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .priority-bar {
    height: 30px;
  }

  .task-meta {
    font-size: 10px;
    gap: 6px;
  }

  .task-meta .el-tag {
    font-size: 10px;
  }

  .panel-header {
    padding: 14px;
  }

  .panel-body {
    padding: 14px;
  }

  .weather-list {
    padding: 6px 14px 14px;
  }

  .weather-day {
    padding: 12px 2px;
  }

  .weather-day b {
    font-size: 18px;
  }

  .weather-day span,
  .weather-day small {
    font-size: 10px;
  }

  .content-grid {
    gap: 12px;
  }
}

@media (max-width: 600px) {
  .task-row {
    grid-template-columns: 3px 1fr;
  }

  .task-meta {
    grid-column: 2;
    justify-self: start;
  }

  .task-row p {
    max-width: none;
  }
}

@media (max-width: 480px) {
  .task-meta {
    display: none;
  }
}
</style>
