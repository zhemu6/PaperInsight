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
const breadcrumbs = ref<BreadcrumbItem[]>([{ id: 0, name: '根目录' }])
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
    ElMessage.error('加载列表失败')
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
  if (!form.name) return ElMessage.warning('请输入名称')
  
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
    ElMessage.success(dialogType.value === 'add' ? '创建成功' : '重命名成功')
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
    ElMessage.success('修改成功')
    editPaperVisible.value = false
    loadFiles()
  } catch (error) {
    //
  }
}

// 删除
const handleDelete = (row: FileItem) => {
  ElMessageBox.confirm(
    '确定要删除该项目吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
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
        ElMessage.success('删除成功')
        loadFiles()
      } catch (e: any) {
        // 捕获 BusinessException, 如果是 FolderNotEmpty
        if (e.message?.includes("FolderNotEmpty") || e.code === 40101 || e.code === 50001) { 
           ElMessageBox.confirm(
             '文件夹不为空，是否递归删除所有内容？此操作进入回收站。',
             '确认递归删除',
             {
               confirmButtonText: '确定',
               cancelButtonText: '取消',
               type: 'warning',
             }
           ).then(async () => {
               await deleteFolder({ id: row.id }, { params: { recursive: true } })
               ElMessage.success('递归删除成功')
               loadFiles()
           }).catch(() => {
               ElMessage.info('已取消删除')
           })
        }
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
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
          <div class="i-ep-folder-add mr-1" /> 新建文件夹
        </el-button>
        <el-button class="!rounded-md" @click="uploadVisible = true">
          <div class="i-ep-upload mr-1" /> 上传论文
        </el-button>
        <el-button class="!rounded-md" @click="recycleVisible = true">
          <div class="i-ep-delete mr-1" /> 回收站
        </el-button>
      </div>
      <!-- 搜索框 -->
      <div class="flex items-center gap-2">
        <el-input 
          v-model="searchText"
          placeholder="搜索文件" 
          style="width: 200px"
          @keyup.enter="loadFiles"
          clearable
          @clear="loadFiles"
        >
          <template #prefix>
            <div class="i-ep-search" />
          </template>
        </el-input>
        <el-button class="!rounded-md" @click="loadFiles" plain title="刷新">
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
      <el-table-column label="名称" min-width="300">
        <template #default="{ row }">
          <div class="flex items-center gap-2 cursor-pointer">
            <div 
              :class="row.type === 'folder' ? 'i-ep-folder text-yellow-400 text-xl' : 'i-ep-document text-blue-400 text-xl'" 
            />
            <span class="font-medium hover:text-blue-500 hover:underline">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="200">
        <template #default="{ row }">
          {{ row.updateTime ? dayjs(row.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="right">
        <template #default="{ row }">
          <div class="flex justify-end gap-2" @click.stop>
            <el-button v-if="row.type === 'folder'" link type="primary" @click="handleRename(row)">重命名</el-button>
            <el-button v-else link type="primary" @click="handleEditPaper(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新建文件夹' : '重命名'"
      width="30%"
    >
      <el-input v-model="form.name" placeholder="请输入文件夹名称" @keyup.enter="handleSubmit" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 论文编辑弹窗 -->
    <el-dialog
      v-model="editPaperVisible"
      title="编辑论文信息"
      width="50%"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editForm.authors" placeholder="多个作者用逗号分隔" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="editForm.keywords" placeholder="空格分隔" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="editForm.abstractInfo" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="公开">
           <el-switch 
              v-model="editForm.isPublic" 
              :active-value="1" 
              :inactive-value="0"
              active-text="公开可见"
              inactive-text="仅自己可见"
            />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editPaperVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePaperSubmit">
            保存
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
