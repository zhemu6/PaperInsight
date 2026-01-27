<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { getPaperDetail } from '~/api/paperController'
import MarkdownViewer from '~/components/InsightPanel/MarkdownViewer.vue'
import ScoreRadarChart from '~/components/InsightPanel/RadarChart.vue'
import PdfViewer from '~/components/PdfViewer/index.vue'

const { t } = useI18n()
const route = useRoute()
const paperId = route.params.id as string

const loading = ref(true)
const detail = ref<API.PaperDetailVO | null>(null)
const activeTab = ref('insight')

async function fetchData() {
  try {
    loading.value = true
    const res = await getPaperDetail({ id: Number(paperId) })
    if (res.code === 0) {
      detail.value = res.data || null
    }
    else {
      ElMessage.error(res.message || t('paperDetail.fetchFailed'))
    }
  }
  catch (error) {
    console.error(error)
    ElMessage.error(t('paperDetail.networkError'))
  }
  finally {
    loading.value = false
  }
}

const scoreData = computed(() => {
  const details = detail.value?.paperInsight?.scoreDetails as any
  return details?.dimensions || {}
})

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div v-loading="loading" class="h-[calc(100vh-60px)] flex flex-row overflow-hidden bg-gray-50 dark:bg-gray-900">
    <!-- Left Panel: PDF Viewer (50%) -->
    <div class="relative flex flex-shrink-0 flex-col border-r border-gray-200 dark:border-gray-700" style="width: 50%">
      <div v-if="detail?.paperInfo?.cosUrl" class="h-full">
        <PdfViewer :url="detail.paperInfo.cosUrl" />
      </div>
      <div v-else class="h-full flex items-center justify-center text-gray-400">
        {{ t('paperDetail.noPdf') }}
      </div>
    </div>

    <!-- Right Panel: Insight Dashboard (50%) -->
    <div class="h-full flex flex-shrink-0 flex-col bg-white shadow-lg dark:bg-gray-800" style="width: 50%">
      <!-- Header -->
      <div class="border-b border-gray-100 p-4 dark:border-gray-700">
        <h1 class="line-clamp-2 mb-2 text-lg text-gray-900 font-bold leading-snug dark:text-gray-100">
          {{ detail?.paperInfo?.title }}
        </h1>
        <div class="flex items-center gap-2 text-xs text-gray-500">
          <el-tag size="small" type="info">
            {{ detail?.paperInfo?.publishDate || 'Unknown Date' }}
          </el-tag>
          <span class="max-w-[200px] truncate">{{ detail?.paperInfo?.authors }}</span>
        </div>
      </div>

      <!-- Scrollable Content -->
      <div class="custom-scrollbar flex-1 overflow-y-auto">
        <!-- Score Card -->
        <div class="mb-2 bg-orange-50/50 p-4 dark:bg-gray-800/50">
          <ScoreRadarChart :data="scoreData" :score="detail?.paperInsight?.score || 0" />
        </div>

        <!-- Tabs -->
        <el-tabs v-model="activeTab" class="px-4">
          <el-tab-pane :label="t('paperDetail.tabs.insight')" name="insight">
            <div class="py-4 space-y-6">
              <!-- Summary -->
              <section>
                <div class="mb-3 flex items-center gap-2">
                  <div class="i-ri-article-line text-lg text-blue-500" />
                  <h3 class="text-gray-800 font-bold dark:text-gray-200">
                    {{ t('paperDetail.sections.summary') }}
                  </h3>
                </div>
                <div class="rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
                  <MarkdownViewer :content="detail?.paperInsight?.summaryMarkdown || ''" />
                </div>
              </section>

              <!-- Innovation -->
              <section>
                <div class="mb-3 flex items-center gap-2">
                  <div class="i-ri-lightbulb-flash-line text-lg text-yellow-500" />
                  <h3 class="text-gray-800 font-bold dark:text-gray-200">
                    {{ t('paperDetail.sections.innovation') }}
                  </h3>
                </div>
                <div class="rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
                  <MarkdownViewer :content="detail?.paperInsight?.innovationPoints || ''" />
                </div>
              </section>

              <!-- Methods -->
              <section>
                <div class="mb-3 flex items-center gap-2">
                  <div class="i-ri-hammer-line text-lg text-purple-500" />
                  <h3 class="text-gray-800 font-bold dark:text-gray-200">
                    {{ t('paperDetail.sections.methods') }}
                  </h3>
                </div>
                <div class="rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
                  <MarkdownViewer :content="detail?.paperInsight?.methods || ''" />
                </div>
              </section>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="t('paperDetail.tabs.chat')" name="chat" class="h-full">
            <div class="h-full">
               <ChatPanel :paper-id="paperId" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #ddd;
  border-radius: 3px;
}
</style>
