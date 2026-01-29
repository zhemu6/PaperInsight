<script setup lang="ts">
import { ArrowRight, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listPublicPaperByPage } from '~/api/paperController'

const { t } = useI18n()

const router = useRouter()
const loading = ref(false)
const searchForm = reactive({
  title: '',
  authors: '',
  keywords: '',
  abstractInfo: '',
  contentKeyword: '',
})
const papers = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

async function fetchPapers() {
  try {
    loading.value = true

    const res = await listPublicPaperByPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm,
      sortField: 'create_time',
      sortOrder: 'descend',
    }) as any

    if (res.code === 0) {
      papers.value = res.data.records
      total.value = Number.parseInt(res.data.total)
    }
    else {
      ElMessage.error(res.message || '获取论文失败')
    }
  }
  catch (error) {
    ElMessage.error('获取论文列表失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchPapers()
}

function resetSearch() {
  searchForm.title = ''
  searchForm.authors = ''
  searchForm.keywords = ''
  searchForm.abstractInfo = ''
  searchForm.contentKeyword = ''
  handleSearch()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchPapers()
}

function formatTime(time: string) {
  if (!time)
    return ''
  return new Date(time).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

// 跳转到论文详情页
function goToDetail(paperId: number) {
  router.push(`/paper/detail/${paperId}`)
}

onMounted(() => {
  fetchPapers()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <div class="mb-6 border border-gray-100 rounded-xl bg-white p-6 shadow-sm transition-shadow duration-300 dark:border-gray-800 dark:bg-gray-800 hover:shadow-md">
      <div class="mb-2 text-sm text-gray-500 font-bold dark:text-gray-400">
        {{ $t('paperSquare.advancedSearch') }}
      </div>
      <el-form :inline="true" :model="searchForm" class="flex flex-wrap items-center gap-2">
        <el-form-item :label="$t('paperSquare.titleLabel')" class="mb-0!">
          <el-input v-model="searchForm.title" :placeholder="$t('paperSquare.titlePlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('paperSquare.authors')" class="mb-0!">
          <el-input v-model="searchForm.authors" :placeholder="$t('paperSquare.authorsPlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('paperSquare.keywords')" class="mb-0!">
          <el-input v-model="searchForm.keywords" :placeholder="$t('paperSquare.keywordsPlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('paperSquare.content')" class="mb-0!">
          <el-input v-model="searchForm.contentKeyword" :placeholder="$t('paperSquare.contentPlaceholder')" clearable style="width: 160px">
            <template #prefix>
              <div class="i-ep-document-search text-gray-400" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="$t('paperSquare.abstract')" class="mb-0!">
          <el-input v-model="searchForm.abstractInfo" :placeholder="$t('paperSquare.abstractPlaceholder')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item class="mb-0!">
          <el-button type="primary" @click="handleSearch">
            {{ $t('common.search') }}
          </el-button>
          <el-button @click="resetSearch">
            {{ $t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="flex-1 overflow-auto">
      <el-row :gutter="20">
        <el-col v-for="paper in papers" :key="paper.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="6" class="mb-4">
          <el-card class="h-full cursor-pointer transition-all duration-300 !border-gray-100 !rounded-xl hover:shadow-lg dark:!border-gray-800" :body-style="{ padding: '0px', height: '100%', display: 'flex', flexDirection: 'column' }" shadow="hover" @click="goToDetail(paper.id)">
            <!-- Cover Image -->
            <div class="group relative h-48 overflow-hidden border-b border-gray-100 bg-gray-50 dark:border-gray-800 dark:bg-gray-800/50">
              <img
                v-if="paper.coverUrl"
                :src="paper.coverUrl"
                class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                loading="lazy"
              >
              <div v-else class="h-full w-full flex items-center justify-center text-gray-300 dark:text-gray-600">
                <div class="i-ep-document text-6xl" />
              </div>
              <!-- Overlay -->
              <div class="absolute inset-0 bg-black/0 transition-colors duration-300 group-hover:bg-black/5" />
            </div>

            <div class="flex flex-1 flex-col p-4">
              <h3 class="line-clamp-2 mb-2 text-lg text-gray-800 font-bold dark:text-gray-100" :title="paper.title">
                {{ paper.title }}
              </h3>
              <div class="mb-3 flex items-center gap-1 text-xs text-gray-500">
                <el-icon><User /></el-icon>
                <span class="truncate">{{ paper.authors || $t('common.unknown') }}</span>
              </div>
              <div class="line-clamp-3 mb-4 flex-1 text-sm text-gray-600 dark:text-gray-400">
                {{ paper.abstractInfo || $t('paperSquare.noAbstract') }}
              </div>
              <div class="mt-auto flex flex-wrap gap-2">
                <span v-for="keyword in (paper.keywords ? paper.keywords.split(' ').slice(0, 3) : [])" :key="keyword" class="rounded-md bg-[var(--el-color-primary-light-9)] px-2 py-1 text-xs text-[var(--el-color-primary)]">
                  {{ keyword }}
                </span>
              </div>
            </div>
            <div class="flex items-center justify-between border-t border-gray-50 bg-gray-50/50 px-4 py-3 text-xs text-gray-400 dark:border-gray-800 dark:bg-gray-800/50">
              <span>{{ formatTime(paper.createTime) }}</span>
              <div class="flex items-center gap-1 text-[var(--el-color-primary)] opacity-0 transition-opacity group-hover:opacity-100">
                <span>{{ $t('paperSquare.viewDetail') }}</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && papers.length === 0" :description="$t('paperSquare.noPapers')" />

      <div class="mt-4 flex justify-center pb-4">
        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
