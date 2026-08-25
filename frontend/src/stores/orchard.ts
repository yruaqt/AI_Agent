import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData } from '@/types'

export const useOrchardStore = defineStore('orchard', () => {
  const orchards = ref<Orchard[]>([])
  const currentId = ref(localStorage.getItem('currentOrchardId') || '')
  const loading = ref(false)
  const current = computed(() => orchards.value.find(item => item.id === currentId.value) || null)

  async function load() {
    if (loading.value) return
    loading.value = true
    try {
      const data = unwrap<PageData<Orchard>>(
        await api.get('/orchards', { params: { pageSize: 50, status: 'ENABLED' } })
      )
      orchards.value = data.items
      if (!data.items.some(item => item.id === currentId.value)) {
        select(data.items[0]?.id || '')
      }
    } finally {
      loading.value = false
    }
  }

  function select(id: string) {
    currentId.value = id
    if (id) localStorage.setItem('currentOrchardId', id)
    else localStorage.removeItem('currentOrchardId')
  }

  return { orchards, currentId, current, loading, load, select }
})
