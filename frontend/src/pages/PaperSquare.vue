<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ArrowRight, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { listPublicPaperByPage } from '~/api/paperController'

const { t } = useI18n()

const router = useRouter()
const loading = ref(false)
const searchForm = reactive({
    title: '',
    authors: '',
    keywords: '',
    abstractInfo: ''
})
const papers = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const fetchPapers = async () => {
    try {
        loading.value = true

        const res = await listPublicPaperByPage({
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            ...searchForm,
            sortField: 'create_time',
            sortOrder: 'descend'
        }) as any

  if (res.code === 0) {
    papers.value = res.data.records
    total.value = Number.parseInt(res.data.total)
  } else {
    ElMessage.error(res.message || '获取论文失败')
  }
} catch (error) {
  ElMessage.error('获取论文列表失败')
} finally {
  loading.value = false
}
}

const handleSearch = () => {
    currentPage.value = 1
    fetchPapers()
}

const resetSearch = () => {
    searchForm.title = ''
    searchForm.authors = ''
    searchForm.keywords = ''
    searchForm.abstractInfo = ''
    handleSearch()
}

const handlePageChange = (page: number) => {
    currentPage.value = page
    fetchPapers()
}

const formatTime = (time: string) => {
    if (!time) return ''
    return new Date(time).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

// 跳转到论文详情页
const goToDetail = (paperId: number) => {
    router.push(`/paper/detail/${paperId}`)
}

onMounted(() => {
    fetchPapers()
})
</script>

<template>
    <div class="h-full flex flex-col p-6">
        <div class="mb-4 bg-white dark:bg-gray-800 p-4 rounded shadow-sm border border-gray-100 dark:border-gray-800">
             <div class="mb-2 font-bold text-sm text-gray-500 dark:text-gray-400">{{ $t('paperSquare.advancedSearch') }}</div>
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
                <el-form-item :label="$t('paperSquare.abstract')" class="mb-0!">
                     <el-input v-model="searchForm.abstractInfo" :placeholder="$t('paperSquare.abstractPlaceholder')" clearable style="width: 160px" />
                </el-form-item>
                <el-form-item class="mb-0!">
                    <el-button type="primary" @click="handleSearch">{{ $t('common.search') }}</el-button>
                     <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div v-loading="loading" class="flex-1 overflow-y-auto overflow-x-hidden min-h-0">
            <el-row v-if="papers.length > 0" :gutter="20">
                <el-col
                    v-for="paper in papers"
                    :key="paper.id"
                    :xs="24" :sm="12" :md="12" :lg="8" :xl="6"
                >
                    <el-card 
                        :body-style="{ padding: '0px' }" 
                        class="h-full hover:shadow-lg transition-shadow duration-300 cursor-pointer flex flex-col"
                        shadow="hover"
                        @click="goToDetail(paper.id)"
                    >
                        <template #header>
                            <div class="flex flex-col gap-1.5">
                                <span class="font-bold truncate text-base" :title="paper.title" style="color: var(--el-text-color-primary);">{{ paper.title }}</span>
                                <div class="text-xs flex items-center" style="color: var(--el-text-color-secondary);">
                                    <el-icon class="mr-1"><User /></el-icon>
                                    <span class="truncate" :title="paper.authors">{{ paper.authors || $t('common.unknown') }}</span>
                                </div>
                            </div>
                        </template>

                        <div class="relative h-48 group overflow-hidden border-b" style="background-color: var(--el-fill-color-extra-light); border-color: var(--el-border-color-lighter);">
                             <img 
                                v-if="paper.coverUrl" 
                                :src="paper.coverUrl" 
                                class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                                style="background-color: var(--el-bg-color);" 
                                loading="lazy"
                            />
                            <div v-else class="w-full h-full flex items-center justify-center" style="background-color: var(--el-fill-color-extra-light); color: var(--el-text-color-placeholder);">
                                <div class="i-ep-document text-6xl" />
                            </div>
                            <!-- Overlay -->
                            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-300" />
                        </div>

                        <div class="p-4 flex flex-col">
                            <!-- Tags -->
                            <div class="flex flex-wrap gap-1 mb-3">
                                <el-tag 
                                    v-for="tag in (paper.keywords ? paper.keywords.split(' ') : [])" 
                                    :key="tag" 
                                    size="small" 
                                    effect="plain"
                                    class="!border-none !bg-indigo-50 !text-indigo-500 dark:!bg-indigo-900/30 dark:!text-indigo-300"
                                >
                                    {{ tag }}
                                </el-tag>
                            </div>

                            <p class="text-sm line-clamp-3 h-15" style="color: var(--el-text-color-regular);">
                                {{ paper.abstractInfo || $t('paperSquare.noAbstract') }}
                            </p>
                        </div>

                        <template #footer>
                            <div class="flex justify-between items-center text-xs" style="color: var(--el-text-color-secondary);">
                                <span>{{ formatTime(paper.createTime) }}</span>
                                <el-button type="primary" link size="small" class="!px-0" @click.stop="goToDetail(paper.id)">
                                    {{ $t('common.viewDetail') }} <el-icon class="ml-1"><ArrowRight /></el-icon>
                                </el-button>
                            </div>
                        </template>
                    </el-card>
                </el-col>
            </el-row>

            <div v-else-if="!loading" class="h-64 flex flex-col items-center justify-center text-gray-400">
                <div class="i-ep-box text-6xl mb-4" />
                <p>{{ $t('paperSquare.noPapers') }}</p>
            </div>
        </div>

        <div class="flex justify-center mt-4 pt-2 border-t border-gray-100 dark:border-gray-800">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                background
                @current-change="handlePageChange"
            />
        </div>
    </div>
</template>
