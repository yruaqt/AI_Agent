<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api,{unwrap} from '@/api'
import type{Orchard,PageData}from '@/types'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
const orchard=ref<Orchard|null>(null),tasks=ref<any[]>([]),loading=ref(false),generating=ref(false),date=ref(new Date().toISOString().slice(0,10))
async function load(){loading.value=true;try{if(!orchard.value){const d=unwrap<PageData<Orchard>>(await api.get('/orchards'));orchard.value=d.items[0]}if(orchard.value)tasks.value=unwrap<PageData<any>>(await api.get(`/tasks?orchardId=${orchard.value.id}&pageSize=50`)).items}finally{loading.value=false}}
async function generate(){if(!orchard.value)return;generating.value=true;try{await api.post(`/orchards/${orchard.value.id}/tasks/generate`,{date:date.value});ElMessage.success('农事任务已生成');await load()}finally{generating.value=false}}
async function setStatus(task:any,status:string){await api.patch(`/tasks/${task.id}/status`,{status});task.status=status;ElMessage.success('状态已更新')}
onMounted(load)
const statusText:Record<string,string>={DRAFT:'待确认',CONFIRMED:'已确认',TODO:'待执行',DOING:'执行中',DONE:'已完成',CANCELLED:'已取消'}
</script>
<template><div><div class="page-title-row"><div><h2>{{date}} 农事安排</h2><p>{{orchard?.name}} · {{tasks.length}} 项任务</p></div><div class="toolbar"><el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD"/><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" :icon="MagicStick" :loading="generating" @click="generate">Agent 生成任务</el-button></div></div><section class="panel" v-loading="loading"><el-table :data="tasks" stripe><el-table-column label="优先级" width="88"><template #default="{row}"><span :class="`priority-${row.priority.toLowerCase()}`">● {{row.priority}}</span></template></el-table-column><el-table-column prop="title" label="任务" min-width="170"><template #default="{row}"><strong>{{row.title}}</strong><div class="cell-sub">{{row.type}} · {{row.suggestedTime}}</div></template></el-table-column><el-table-column prop="content" label="执行内容" min-width="300" show-overflow-tooltip/><el-table-column prop="basis" label="依据" min-width="180" show-overflow-tooltip/><el-table-column label="状态" width="108"><template #default="{row}"><el-tag effect="plain" size="small">{{statusText[row.status]}}</el-tag></template></el-table-column><el-table-column label="操作" width="172" fixed="right"><template #default="{row}"><el-dropdown @command="(s:string)=>setStatus(row,s)"><el-button size="small">流转状态</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="CONFIRMED">确认</el-dropdown-item><el-dropdown-item command="TODO">待执行</el-dropdown-item><el-dropdown-item command="DOING">执行中</el-dropdown-item><el-dropdown-item command="DONE">完成</el-dropdown-item><el-dropdown-item command="CANCELLED" divided>取消</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column></el-table></section></div></template>
<style scoped>.cell-sub{font-size:11px;color:var(--muted);margin-top:4px}.priority-high{color:var(--red)}.priority-medium{color:var(--amber)}.priority-low{color:var(--green)}</style>

