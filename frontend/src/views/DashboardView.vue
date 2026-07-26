<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import api,{unwrap} from '@/api'
import type { Orchard,PageData } from '@/types'
import { Refresh, ArrowRight, Cherry, Clock } from '@element-plus/icons-vue'
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
        api.get(`/orchards/${orchard.value.id}/weather?days=3`).catch(() => null),
        api.get(`/tasks?orchardId=${orchard.value.id}&pageSize=8`).catch(() => null)
      ])
      weather.value=w?unwrap<any>(w):null
      tasks.value=t?unwrap<PageData<any>>(t).items:[]
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

const taskStatusMap:Record<string,string>={
  DRAFT:'草稿',
  CONFIRMED:'已确认',
  TODO:'待执行',
  DOING:'执行中',
  DONE:'已完成',
  CANCELLED:'已取消'
}

const priorityMap:Record<string,string>={
  HIGH:'高',
  MEDIUM:'中',
  LOW:'低'
}

function getStatusTagType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    CONFIRMED: 'success',
    TODO: 'warning',
    DOING: 'primary',
    DONE: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

function formatUpdateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚更新'
  if (minutes < 60) return `${minutes} 分钟前更新`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前更新`
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function getWeatherIcon(weather: string): string {
  if (!weather) return '🌤️'
  if (weather.includes('雷') || weather.includes('雷暴')) return '⛈️'
  if (weather.includes('大雨') || weather.includes('暴雨')) return '🌧️'
  if (weather.includes('雨')) return '🌦️'
  if (weather.includes('雪')) return '🌨️'
  if (weather.includes('雾') || weather.includes('霾')) return '🌫️'
  if (weather.includes('阴')) return '☁️'
  if (weather.includes('多云')) return '⛅'
  if (weather.includes('晴')) return '☀️'
  return '🌤️'
}

function getDayLabel(dateStr: string): string {
  if (!dateStr) return ''
  const today = new Date().toISOString().slice(0, 10)
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10)
  const dayAfter = new Date(Date.now() + 86400000 * 2).toISOString().slice(0, 10)
  if (dateStr === today) return '今天'
  if (dateStr === tomorrow) return '明天'
  if (dateStr === dayAfter) return '后天'
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const date = new Date(dateStr)
  return weekdays[date.getDay()]
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
          :icon="Cherry"
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
          <span class="trend">
            {{ weather?.provider || '等待查询' }}
            <span v-if="weather?.updatedAt" class="update-time">{{ formatUpdateTime(weather.updatedAt) }}</span>
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
              class="task-item"
            >
              <div class="task-left">
                <span :class="['priority-dot', `priority-${task.priority?.toLowerCase()}`]"></span>
              </div>
              <div class="task-content">
                <div class="task-title-row">
                  <h4 class="task-title">{{ task.title }}</h4>
                  <span :class="['task-priority-tag', `tag-${task.priority?.toLowerCase()}`]">
                    {{ priorityMap[task.priority] || task.priority }}优先级
                  </span>
                </div>
                <p class="task-desc">{{ task.content }}</p>
                <div class="task-footer">
                  <span class="task-time">
                    <el-icon><Clock /></el-icon>
                    {{ task.suggestedTime || '建议时间待确认' }}
                  </span>
                  <el-tag size="small" :type="getStatusTagType(task.status)">
                    {{ taskStatusMap[task.status] || task.status }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </section>

        <aside class="panel weather-panel">
          <header class="panel-header">
            <h3>天气信息</h3>
            <span v-if="weather?.updatedAt" class="update-badge">{{ formatUpdateTime(weather.updatedAt) }}</span>
          </header>
          <div class="weather-content">
            <div v-if="!weather || (!weather.forecast && !weather.forecasts)" class="weather-empty">
              <EmptyState
                size="small"
                title="暂无天气数据"
                description="天气服务暂时不可用或尚未查询。"
                :show-action="true"
                action-text="刷新重试"
                @action="load"
              />
            </div>
            <template v-else>
              <div class="current-weather" v-if="weather.current">
                <div class="current-left">
                  <span class="current-icon">{{ getWeatherIcon(weather.current.weather) }}</span>
                  <div class="current-info">
                    <div class="current-temp">{{ weather.current.temperatureC }}°C</div>
                    <div class="current-desc">{{ weather.current.weather }}</div>
                  </div>
                </div>
                <div class="current-right">
                  <div class="current-detail">
                    <span class="detail-label">风向</span>
                    <span class="detail-value">{{ weather.current.windDirection }}风</span>
                  </div>
                  <div class="current-detail">
                    <span class="detail-label">风力</span>
                    <span class="detail-value">{{ weather.current.windLevel }}级</span>
                  </div>
                </div>
              </div>

              <div class="forecast-section">
                <div class="section-title">未来三天</div>
                <div class="forecast-grid">
                  <div
                    v-for="day in (weather.forecast || weather.forecasts || [])"
                    :key="day.date || day.dateStr"
                    class="forecast-card"
                  >
                    <div class="forecast-day">
                      <span class="day-label">{{ getDayLabel(day.date || day.dateStr) }}</span>
                      <span class="day-date">{{ (day.date || day.dateStr)?.slice(5) }}</span>
                    </div>
                    <div class="forecast-icon">{{ getWeatherIcon(day.dayWeather || day.phenomenon) }}</div>
                    <div class="forecast-weather">
                      <span class="day-weather">{{ day.dayWeather || day.phenomenon }}</span>
                      <span class="night-weather">夜 {{ day.nightWeather || '—' }}</span>
                    </div>
                    <div class="forecast-temp">
                      <span class="temp-high">{{ day.maxTemperatureC || day.maxTemperature }}°</span>
                      <span class="temp-divider">/</span>
                      <span class="temp-low">{{ day.minTemperatureC || day.minTemperature }}°</span>
                    </div>
                    <div class="forecast-wind" v-if="day.windLevel">
                      {{ day.windLevel }}级风
                    </div>
                  </div>
                </div>
              </div>

              <el-alert
                v-if="weather?.warning"
                :title="weather.warning"
                type="warning"
                :closable="false"
                show-icon
                class="weather-warning"
              />
            </template>
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
  padding: 8px 18px 18px;
}

.task-empty {
  padding: 8px 0;
}

.task-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.task-item:last-child {
  border-bottom: 0;
}

.task-left {
  flex-shrink: 0;
  padding-top: 4px;
}

.priority-dot {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--green);
  flex-shrink: 0;
  margin-top: 4px;
}

.priority-dot.priority-high {
  background: var(--red);
  box-shadow: 0 0 0 3px rgba(179, 74, 67, 0.15);
}

.priority-dot.priority-medium {
  background: var(--amber);
  box-shadow: 0 0 0 3px rgba(197, 137, 50, 0.15);
}

.priority-dot.priority-low {
  background: var(--green);
  box-shadow: 0 0 0 3px rgba(46, 107, 78, 0.15);
}

.task-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.task-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.task-priority-tag {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 3px;
  font-weight: 600;
}

.task-priority-tag.tag-high {
  background: #fef0ef;
  color: var(--red);
}

.task-priority-tag.tag-medium {
  background: #fff8ed;
  color: var(--amber);
}

.task-priority-tag.tag-low {
  background: #edf4ef;
  color: var(--green);
}

.task-desc {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.task-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--muted);
  font-size: 12px;
}

.task-time .el-icon {
  font-size: 14px;
}

.weather-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.weather-panel .panel-header {
  flex-shrink: 0;
}

.weather-content {
  padding: 4px 18px 18px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.weather-empty {
  padding: 8px 0;
}

.current-weather {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px;
  background: linear-gradient(135deg, var(--green-light) 0%, #f0f7f2 100%);
  border-radius: 8px;
  margin: 14px 0 20px;
}

.current-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.current-icon {
  font-size: 48px;
  line-height: 1;
}

.current-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.current-temp {
  font-size: 32px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1;
}

.current-desc {
  font-size: 13px;
  color: var(--muted);
}

.current-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.current-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.detail-label {
  font-size: 11px;
  color: var(--muted);
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.forecast-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  margin-bottom: 10px;
  padding-left: 2px;
}

.forecast-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  flex: 1;
}

.forecast-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 18px 8px;
  background: #fafbf9;
  border: 1px solid var(--line);
  border-radius: 8px;
  text-align: center;
  min-height: 150px;
}

.forecast-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.day-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.day-date {
  font-size: 11px;
  color: var(--muted);
}

.forecast-icon {
  font-size: 34px;
  line-height: 1;
}

.forecast-weather {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.day-weather {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

.night-weather {
  font-size: 11px;
  color: var(--muted);
}

.forecast-temp {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.temp-high {
  font-weight: 600;
  color: var(--ink);
}

.temp-divider {
  color: var(--muted);
}

.temp-low {
  color: var(--muted);
}

.forecast-wind {
  font-size: 11px;
  color: var(--muted);
}

.weather-warning {
  margin-top: 16px;
}

.update-badge {
  font-size: 11px;
  color: var(--muted);
}

.update-time {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-top: 2px;
}

.empty-inline {
  padding: 30px;
  color: var(--muted);
  text-align: center;
  font-size: 13px;
}

@media (max-width: 760px) {
  .task-list {
    padding: 8px 14px 14px;
  }

  .task-item {
    gap: 12px;
    padding: 14px 0;
  }

  .task-title {
    font-size: 13px;
  }

  .task-desc {
    font-size: 12px;
    -webkit-line-clamp: 2;
  }

  .priority-dot {
    width: 8px;
    height: 8px;
  }

  .task-time {
    font-size: 11px;
  }

  .panel-header {
    padding: 14px;
  }

  .panel-body {
    padding: 14px;
  }

  .weather-content {
    padding: 4px 14px 14px;
  }

  .current-weather {
    padding: 16px 14px;
    margin: 12px 0 16px;
  }

  .current-icon {
    font-size: 40px;
  }

  .current-temp {
    font-size: 26px;
  }

  .forecast-card {
    padding: 12px 6px;
    gap: 6px;
  }

  .forecast-icon {
    font-size: 24px;
  }

  .day-weather {
    font-size: 11px;
  }

  .content-grid {
    gap: 12px;
  }
}

@media (max-width: 600px) {
  .task-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-priority-tag {
    align-self: flex-start;
  }
}

@media (max-width: 480px) {
  .task-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
