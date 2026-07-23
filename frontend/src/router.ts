import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import AppShell from '@/components/AppShell.vue'

const router=createRouter({history:createWebHistory(),routes:[
  {path:'/login',component:LoginView},
  {path:'/',component:AppShell,children:[
    {path:'',name:'dashboard',component:()=>import('@/views/DashboardView.vue')},
    {path:'chat',name:'chat',component:()=>import('@/views/ChatView.vue')},
    {path:'tasks',name:'tasks',component:()=>import('@/views/TasksView.vue')},
    {path:'calculators',name:'calculators',component:()=>import('@/views/CalculatorsView.vue')},
    {path:'records',name:'records',component:()=>import('@/views/RecordsView.vue')},
    {path:'orchards',name:'orchards',component:()=>import('@/views/OrchardsView.vue')},
    {path:'knowledge',name:'knowledge',component:()=>import('@/views/KnowledgeView.vue'),meta:{admin:true}},
    {path:'users',name:'users',component:()=>import('@/views/UsersView.vue'),meta:{admin:true}}
  ]}
]})
router.beforeEach(to=>{const token=localStorage.getItem('accessToken');if(to.path!=='/login'&&!token)return'/login';if(to.path==='/login'&&token)return'/'})
export default router

