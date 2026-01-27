<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { 
  addFolder, 
  deleteFolder, 
  updateFolder, 
  listFolderByPage 
} from '~/api/folderController'
import { listPaperByPage, deletePaper, updatePaper } from '~/api/paperController'
import RecycleBin from './RecycleBin.vue'
import PaperUpload from './PaperUpload.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface BreadcrumbItem {
  id: number
  name: string
}

interface FileItem {
  id: number
  name: string
  type: 'folder' | 'paper'
  updateTime?: string
  // 其他属性...
}

const loading = ref(false)
const currentParentId = ref<number>(0)
const breadcrumbs = ref<BreadcrumbItem[]>([{ id: 0, name: t('file.manager.root') }])
const fileList = ref<FileItem[]>([])
const searchText = ref('')

// 弹窗控制
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'rename'>('add')
const recycleVisible = ref(false)
const uploadVisible = ref(false)
const form = reactive({
  id: 0,
  name: '',
})

// 加载文件列表
const loadFiles = async () => {
  loading.value = true
  try {
    // 1. 获取文件夹
    const folderRes = await listFolderByPage({
      parentId: currentParentId.value,
      name: searchText.value,
      pageSize: 100, // 暂时不分页，查多一点
      current: 1
    })
    
    // 注意：后端返回的是 Page<FolderVO>，这里通过 .data 获取
    // 如果后端 Response 结构是 { code: 0, data: { records: [...] } }
    const folders: FileItem[] = (folderRes.data?.records || []).map((item: any) => ({
      id: item.id,
      name: item.name,
      type: 'folder',
      updateTime: item.updateTime
    }))

    // 2. 获取论文
    const paperRes = await listPaperByPage({
      folderId: currentParentId.value,
      title: searchText.value,
      pageSize: 100,
      current: 1
    })
    
    const papers: FileItem[] = (paperRes.data?.records || []).map((item: any) => ({
      id: item.id,
      name: item.title,
      type: 'paper',
      updateTime: item.updateTime,
      // Extra fields for edit
      authors: item.authors,
      keywords: item.keywords,
      abstractInfo: item.abstractInfo,
      isPublic: item.isPublic,
      folderId: item.folderId
    }))

    fileList.value = [...folders, ...papers]
  } catch (error: any) {
    ElMessage.error(t('file.manager.actions.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 进入文件夹
const handleEnterFolder = (row: FileItem) => {
  if (row.type === 'folder') {
    currentParentId.value = row.id
    breadcrumbs.value.push({ id: row.id, name: row.name })
    loadFiles()
  }
}

// 面包屑导航
const handleBreadcrumbClick = (item: BreadcrumbItem, index: number) => {
  currentParentId.value = item.id
  breadcrumbs.value = breadcrumbs.value.slice(0, index + 1)
  loadFiles()
}

// 新建文件夹
const handleCreateFolder = () => {
  dialogType.value = 'add'
  form.name = ''
  dialogVisible.value = true
}

// 重命名
const handleRename = (row: FileItem) => {
  dialogType.value = 'rename'
  form.id = row.id
  form.name = row.name
  dialogVisible.value = true
}

// 提交表单 (文件夹)
const handleSubmit = async () => {
  if (!form.name) return ElMessage.warning(t('file.manager.actions.inputName'))
  
  try {
    if (dialogType.value === 'add') {
      await addFolder({
        name: form.name,
        parentId: currentParentId.value
      })
    } else {
      await updateFolder({
        id: form.id,
        name: form.name
      })
    }
    ElMessage.success(dialogType.value === 'add' ? t('file.manager.actions.createSuccess') : t('file.manager.actions.renameSuccess'))
    dialogVisible.value = false
    loadFiles()
  } catch (error: any) {
    // 错误在 request.ts 已统一提示，这里无需处理，或处理特定逻辑
  }
}

// 论文编辑相关
const editPaperVisible = ref(false)
const editForm = reactive({
  id: 0,
  title: '',
  authors: '',
  keywords: '',
  abstractInfo: '',
  isPublic: 0,
  folderId: 0
})

const handleEditPaper = (row: any) => {
  editPaperVisible.value = true
  // 初始化表单
  editForm.id = row.id
  editForm.title = row.name 
  editForm.authors = row.authors || ''
  editForm.keywords = row.keywords || ''
  editForm.abstractInfo = row.abstractInfo || ''
  editForm.isPublic = row.isPublic || 0
  editForm.folderId = row.folderId || currentParentId.value
}

const handlePaperSubmit = async () => {
  try {
    await updatePaper({
      id: editForm.id,
      title: editForm.title,
      authors: editForm.authors,
      keywords: editForm.keywords,
      abstractInfo: editForm.abstractInfo,
      isPublic: editForm.isPublic,
      folderId: editForm.folderId
    })
    ElMessage.success(t('user.profileInfo.saveSuccess'))
    editPaperVisible.value = false
    loadFiles()
  } catch (error) {
    //
  }
}

// 删除
const handleDelete = (row: FileItem) => {
  ElMessageBox.confirm(
    t('file.manager.actions.confirmDelete'),
    t('common.warning'),
    {
      confirmButtonText: t('file.manager.dialog.confirm'),
      cancelButtonText: t('file.manager.dialog.cancel'),
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        if (row.type === 'folder') {
            await deleteFolder({ id: row.id })
        } else {
            await deletePaper({ id: row.id })
        }
        ElMessage.success(t('file.manager.actions.deleteSuccess'))
        loadFiles()
      } catch (e: any) {
        // 捕获 BusinessException, 如果是 FolderNotEmpty
        if (e.message?.includes("FolderNotEmpty") || e.code === 40101 || e.code === 50001) { 
           ElMessageBox.confirm(
             t('file.manager.actions.folderNotEmpty'),
             t('file.manager.actions.recursiveDelete'),
             {
               confirmButtonText: t('file.manager.dialog.confirm'),
               cancelButtonText: t('file.manager.dialog.cancel'),
               type: 'warning',
             }
           ).then(async () => {
               await deleteFolder({ id: row.id }, { params: { recursive: true } })
               ElMessage.success(t('file.manager.actions.recursionSuccess'))
               loadFiles()
           }).catch(() => {
               ElMessage.info(t('file.manager.actions.cancelDelete'))
           })
        }
      }
    })
    .catch(() => {
      ElMessage.info(t('file.manager.actions.cancelDelete'))
    })
}

onMounted(() => {
  loadFiles()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 顶部工具栏 -->
    <div class="mb-4 flex justify-between items-center w-full">
      <div class="flex items-center gap-4">
        <el-button type="primary" class="!rounded-md" @click="handleCreateFolder">
          <div class="i-ep-folder-add mr-1" /> {{ t('file.manager.newFolder') }}
        </el-button>
        <el-button class="!rounded-md" @click="uploadVisible = true">
          <div class="i-ep-upload mr-1" /> {{ t('file.manager.uploadPaper') }}
        </el-button>
        <el-button class="!rounded-md" @click="recycleVisible = true">
          <div class="i-ep-delete mr-1" /> {{ t('file.manager.recycleBin') }}
        </el-button>
      </div>
      <!-- 搜索框 -->
      <div class="flex items-center gap-2">
        <el-input 
          v-model="searchText"
          :placeholder="t('file.manager.searchPlaceholder')" 
          style="width: 200px"
          @keyup.enter="loadFiles"
          clearable
          @clear="loadFiles"
        >
          <template #prefix>
            <div class="i-ep-search" />
          </template>
        </el-input>
        <el-button class="!rounded-md" @click="loadFiles" plain :title="t('file.manager.refresh')">
            <div class="i-ep-refresh" />
        </el-button>
      </div>
    </div>

    <!-- 面包屑 -->
    <div class="mb-4 px-2 py-1 bg-white dark:bg-dark-800 rounded border border-gray-200 dark:border-gray-700">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item 
          v-for="(item, index) in breadcrumbs" 
          :key="item.id"
          class="cursor-pointer hover:text-blue-500"
          @click="handleBreadcrumbClick(item, index)"
        >
          {{ item.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 文件列表 -->
    <el-table 
      v-loading="loading"
      :data="fileList" 
      style="width: 100%" 
      class="flex-1"
      @row-click="handleEnterFolder"
    >
      <el-table-column :label="t('file.manager.columns.name')" min-width="300">
        <template #default="{ row }">
          <div class="flex items-center gap-2 cursor-pointer">
            <div 
              :class="row.type === 'folder' ? 'i-ep-folder text-yellow-400 text-xl' : 'i-ep-document text-blue-400 text-xl'" 
            />
            <span class="font-medium hover:text-blue-500 hover:underline">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('file.manager.columns.updateTime')" width="200">
        <template #default="{ row }">
          {{ row.updateTime ? dayjs(row.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('file.manager.columns.operation')" width="150" align="right">
        <template #default="{ row }">
          <div class="flex justify-end gap-2" @click.stop>
            <el-button v-if="row.type === 'folder'" link type="primary" @click="handleRename(row)">{{ t('file.manager.actions.rename') }}</el-button>
            <el-button v-else link type="primary" @click="handleEditPaper(row)">{{ t('file.manager.actions.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('file.manager.actions.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? t('file.manager.dialog.createTitle') : t('file.manager.dialog.renameTitle')"
      width="30%"
    >
      <el-input v-model="form.name" :placeholder="t('file.manager.dialog.inputPlaceholder')" @keyup.enter="handleSubmit" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('file.manager.dialog.cancel') }}</el-button>
          <el-button type="primary" @click="handleSubmit">
            {{ t('file.manager.dialog.confirm') }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 论文编辑弹窗 -->
    <el-dialog
      v-model="editPaperVisible"
      :title="t('file.manager.dialog.editPaperTitle')"
      width="50%"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item :label="t('file.manager.edit.title')">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item :label="t('file.manager.edit.authors')">
          <el-input v-model="editForm.authors" :placeholder="t('file.manager.edit.authorsPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('file.manager.edit.keywords')">
          <el-input v-model="editForm.keywords" :placeholder="t('file.manager.edit.keywordsPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('file.manager.edit.abstract')">
          <el-input v-model="editForm.abstractInfo" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('file.manager.edit.public')">
           <el-switch 
              v-model="editForm.isPublic" 
              :active-value="1" 
              :inactive-value="0"
              :active-text="t('file.manager.edit.publicYes')"
              :inactive-text="t('file.manager.edit.publicNo')"
            />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editPaperVisible = false">{{ t('file.manager.dialog.cancel') }}</el-button>
          <el-button type="primary" @click="handlePaperSubmit">
            {{ t('file.manager.dialog.save') }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <RecycleBin v-model:visible="recycleVisible" @close="loadFiles" />
    <PaperUpload
      v-model:visible="uploadVisible"
      :folder-id="currentParentId"
      @success="loadFiles"
    />
  </div>
</template>

<style scoped>
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
