<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '~/request'

const { t } = useI18n()
const journalName = ref('')
const loading = ref(false)
const rankData = ref<any>(null)
const hasSearched = ref(false)

async function searchJournal() {
  if (!journalName.value.trim()) {
    ElMessage.warning(t('journal.message.inputRequired'))
    return
  }

  loading.value = true
  hasSearched.value = true
  rankData.value = null

  try {
    const res = await request.get('/journal/rank', {
      params: { name: journalName.value.trim() },
    })
    if (res.code === 0 && res.data) {
      rankData.value = res.data
    }
  }
  catch {
    // 错误已被拦截器处理
  }
  finally {
    loading.value = false
  }
}

// 获取等级标签颜色
function getTagType(key: string, value: string): any {
  if (key === 'sciwarn')
    return 'danger'
  if (['Q1', '1区', 'A', 'A+', 'A++', 'TOP', 'AAA'].includes(value))
    return 'danger'
  if (['Q2', '2区', 'B', 'AA'].includes(value))
    return 'warning'
  if (['Q3', '3区', 'C'].includes(value))
    return 'info'
  return ''
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 搜索区域 -->
    <div class="mb-6 border border-gray-100 rounded-xl bg-white p-6 shadow-sm transition-shadow duration-300 dark:border-gray-800 dark:bg-gray-800 hover:shadow-md">
      <h2 class="mb-4 flex items-center gap-2 text-lg font-bold">
        <div class="i-ep-search text-primary text-xl" />
        {{ t('journal.title') }}
      </h2>
      <p class="mb-4 text-sm text-gray-500 dark:text-gray-400">
        {{ t('journal.subtitle') }}
      </p>

      <div class="flex gap-3">
        <el-input
          v-model="journalName"
          :placeholder="t('journal.placeholder')"
          size="large"
          clearable
          class="flex-1"
          @keyup.enter="searchJournal"
        />
        <el-button
          type="primary"
          size="large"
          :icon="Search"
          :loading="loading"
          @click="searchJournal"
        >
          {{ t('journal.search') }}
        </el-button>
      </div>
    </div>

    <!-- 结果展示区域 -->
    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <el-icon class="is-loading text-primary text-4xl">
        <div class="i-ep-loading" />
      </el-icon>
    </div>

    <div v-else-if="rankData" class="flex-1 overflow-auto">
      <div class="border border-gray-100 rounded-xl bg-white p-6 shadow-sm transition-shadow duration-300 dark:border-gray-800 dark:bg-gray-800 hover:shadow-md">
        <h3 class="mb-4 border-b border-gray-100 pb-3 text-base font-bold dark:border-gray-700">
          {{ t('journal.result.title') }}
        </h3>

        <!-- 官方数据集 -->
        <div v-if="rankData.officialRank?.all && Object.keys(rankData.officialRank.all).length > 0">
          <h4 class="mb-3 text-sm text-gray-600 font-medium dark:text-gray-400">
            {{ t('journal.result.official') }}
          </h4>
          <div class="mb-6 flex flex-wrap gap-2">
            <el-tag
              v-for="(value, key) in rankData.officialRank.all"
              :key="key"
              :type="getTagType(key as string, value as string)"
              size="large"
              class="!px-3 !py-1"
            >
              <span class="font-medium">{{ $te(`journal.rank.${key}`) ? $t(`journal.rank.${key}`) : key }}:</span>
              <span class="ml-1">{{ value }}</span>
            </el-tag>
          </div>
        </div>

        <!-- 自定义数据集 -->
        <div v-if="rankData.customRank?.rank?.length > 0">
          <h4 class="mb-3 text-sm text-gray-600 font-medium dark:text-gray-400">
            {{ t('journal.result.custom') }}
          </h4>
          <div class="flex flex-wrap gap-2">
            <template v-for="rankStr in rankData.customRank.rank" :key="rankStr">
              <el-tag
                v-if="rankStr"
                size="large"
                effect="plain"
                class="!px-3 !py-1"
              >
                {{ (() => {
                  const [uuid, level] = (rankStr as string).split('&&&')
                  const info = rankData.customRank.rankInfo?.find((r: any) => r.uuid === uuid)
                  if (!info) return rankStr
                  const levelTexts = ['oneRankText', 'twoRankText', 'threeRankText', 'fourRankText', 'fiveRankText']
                  const levelText = info[levelTexts[Number(level) - 1]] || level
                  return `${info.abbName}: ${levelText}`
                })() }}
              </el-tag>
            </template>
          </div>
        </div>

        <!-- 无数据 -->
        <div
          v-if="(!rankData.officialRank?.all || Object.keys(rankData.officialRank.all).length === 0) && (!rankData.customRank?.rank || rankData.customRank.rank.length === 0)"
          class="py-8 text-center text-gray-400"
        >
          <div class="i-ep-warning mx-auto mb-2 text-4xl" />
          <p>{{ t('journal.result.empty') }}</p>
        </div>
      </div>
    </div>

    <!-- 未搜索提示 -->
    <div v-else-if="!hasSearched" class="flex flex-1 flex-col items-center justify-center text-gray-400">
      <div class="i-ep-notebook mb-4 text-6xl" />
      <p>{{ t('journal.result.initial') }}</p>
    </div>

    <!-- 搜索无结果 -->
    <div v-else class="flex flex-1 flex-col items-center justify-center text-gray-400">
      <div class="i-ep-document-delete mb-4 text-6xl" />
      <p>{{ t('journal.result.notFound') }}</p>
    </div>

    <!-- 底部致谢 -->
    <div class="mt-4 text-center">
      <a
        href="https://www.easyscholar.cc/"
        target="_blank"
        class="hover:text-primary flex items-center justify-center gap-1 text-xs text-gray-400 transition-colors"
      >
        <div class="i-ep-link" />
        {{ t('journal.credit') }}
      </a>
    </div>
  </div>
</template>
