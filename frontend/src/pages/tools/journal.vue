<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '~/request'

const { t } = useI18n()
const journalName = ref('')
const loading = ref(false)
const rankData = ref<any>(null)
const hasSearched = ref(false)

const searchJournal = async () => {
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
  catch (e: any) {
    // 错误已被拦截器处理
  }
  finally {
    loading.value = false
  }
}

// 获取等级标签颜色
const getTagType = (key: string, value: string): string => {
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
    <div class="mb-6 bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow duration-300 border border-gray-100 dark:border-gray-800">
      <h2 class="text-lg font-bold mb-4 flex items-center gap-2">
        <div class="i-ep-search text-xl text-primary" />
        {{ t('journal.title') }}
      </h2>
      <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
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
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <el-icon class="is-loading text-4xl text-primary">
        <div class="i-ep-loading" />
      </el-icon>
    </div>

    <div v-else-if="rankData" class="flex-1 overflow-auto">
      <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow duration-300 border border-gray-100 dark:border-gray-800">
        <h3 class="text-base font-bold mb-4 pb-3 border-b border-gray-100 dark:border-gray-700">
          {{ t('journal.result.title') }}
        </h3>

        <!-- 官方数据集 -->
        <div v-if="rankData.officialRank?.all && Object.keys(rankData.officialRank.all).length > 0">
          <h4 class="text-sm font-medium text-gray-600 dark:text-gray-400 mb-3">
            {{ t('journal.result.official') }}
          </h4>
          <div class="flex flex-wrap gap-2 mb-6">
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
          <h4 class="text-sm font-medium text-gray-600 dark:text-gray-400 mb-3">
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
          class="text-center text-gray-400 py-8"
        >
          <div class="i-ep-warning text-4xl mb-2 mx-auto" />
          <p>{{ t('journal.result.empty') }}</p>
        </div>
      </div>
    </div>

    <!-- 未搜索提示 -->
    <div v-else-if="!hasSearched" class="flex-1 flex flex-col items-center justify-center text-gray-400">
      <div class="i-ep-notebook text-6xl mb-4" />
      <p>{{ t('journal.result.initial') }}</p>
    </div>

    <!-- 搜索无结果 -->
    <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400">
      <div class="i-ep-document-delete text-6xl mb-4" />
      <p>{{ t('journal.result.notFound') }}</p>
    </div>

    <!-- 底部致谢 -->
    <div class="mt-4 text-center">
      <a
        href="https://www.easyscholar.cc/"
        target="_blank"
        class="text-xs text-gray-400 hover:text-primary transition-colors flex items-center justify-center gap-1"
      >
        <div class="i-ep-link" />
        {{ t('journal.credit') }}
      </a>
    </div>
  </div>
</template>
