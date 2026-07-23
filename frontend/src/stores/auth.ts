import { defineStore } from 'pinia'
import api, { unwrap } from '@/api'
import type { User } from '@/types'

export const useAuthStore=defineStore('auth',{
  state:()=>({ user:null as User|null, loading:false }),
  getters:{ isAdmin:s=>s.user?.role==='ADMIN' },
  actions:{
    async login(username:string,password:string){this.loading=true;try{const data=unwrap<any>(await api.post('/auth/login',{username,password}));localStorage.setItem('accessToken',data.accessToken);this.user=data.user;return data}finally{this.loading=false}},
    async load(){if(!localStorage.getItem('accessToken'))return;this.user=unwrap<User>(await api.get('/auth/me'))},
    logout(){localStorage.removeItem('accessToken');this.user=null}
  }
})

