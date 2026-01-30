<script setup lang="ts">
import { ElSkeleton } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLoginUser } from '~/api/sysUserController'

import Landing from '~/pages/common/landing.vue'

const router = useRouter()

const checking = ref(true)
const isAuthed = ref(false)

const showLanding = computed(() => !checking.value && !isAuthed.value)

onMounted(async () => {
  try {
    const res = await getLoginUser() as any
    if (res?.code === 0 && res?.data?.id) {
      isAuthed.value = true
      router.replace('/file')
      // keep skeleton while redirecting
    }
  }
  catch {
    // not logged in
  }
  finally {
    checking.value = false
  }
})
</script>

<template>
  <Landing v-if="showLanding" />
  <div v-else class="min-h-screen w-full bg-white p-8 dark:bg-gray-900">
    <div class="mx-auto max-w-6xl">
      <ElSkeleton :rows="8" animated />
    </div>
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
</route>
