<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRecycleBin, restore, physicalDelete } from '~/api/recycleBinController'
import { listFolderByPage } from '~/api/folderController'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits(['update:visible', 'close'])

const loading = ref(false)
const list = ref<any[]>([])

// 加载回收站 (只展示论文)
const loadData = async () => {
  loading.value = true
  try {
    const res = await listRecycleBin()
    // 后端返回的是 List<PaperVO>
    if (res.data) {
       list.value = (res.data as any[]).map((item: any) => ({
        ...item,
        type: 'paper',
        displayName: item.title
      }))
    } else {
        list.value = []
    }
  } catch (error) {
    // error handled globally
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

// 还原
import { listFolderByPage } from '~/api/folderController'

// ...

const restoreDialogVisible = ref(false)
const restoreLoading = ref(false)
const targetFolderId = ref(0)
const folderTree = ref<any[]>([])
const currentRestoreRow = ref<any>(null)

// 构建文件夹树
const buildTree = (folders: any[]) => {
  const map = new Map<number, any>()
  const tree: any[] = []
  
  // 1. Initialize map
  folders.forEach(f => {
    map.set(f.id, { ...f, label: f.name, value: f.id, children: [] })
  })

  // 2. Build Hierarchy
  folders.forEach(f => {
    const node = map.get(f.id)
    if (f.parentId && f.parentId !== 0) {
      const parent = map.get(f.parentId)
      if (parent) {
        parent.children.push(node)
      } else {
        // Parent not found (maybe filtered out or root?), treat as root for now?
        // Or just ignore if data consistency is guaranteed.
        // Let's treat as root if parent is missing to be safe
        tree.push(node)
      }
    } else {
      tree.push(node)
    }
  })

  return [
      { 
          label: '根目录', 
          value: 0, 
          children: tree 
      }
  ]
}

// 还原 - 打开选择框
const handleRestore = async (row: any) => {
  currentRestoreRow.value = row
  targetFolderId.value = 0 // 默认根目录
  restoreDialogVisible.value = true
  
  // 加载文件夹列表供选择
  try {
     const res = await listFolderByPage({ pageSize: 1000 })
     if (res.data && res.data.records) {
       folderTree.value = buildTree(res.data.records)
     }
  } catch (e) {
      // ignore
  }
}

// 确认还原
const confirmRestore = async () => {
    if (!currentRestoreRow.value) return
    restoreLoading.value = true
    try {
        await restore({ 
            paperId: currentRestoreRow.value.id, 
            folderId: targetFolderId.value 
        })
        ElMessage.success('还原成功')
        restoreDialogVisible.value = false
        loadData()
        emit('close') 
    } catch(e) {
        //
    } finally {
        restoreLoading.value = false
    }
}

// 彻底删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '此操作将永久删除该文件，不可恢复！确定继续吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await physicalDelete({ paperId: row.id })
      ElMessage.success('彻底删除成功')
      loadData()
    } catch (error) {
        //
    }
  })
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadData()
  }
})
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
}

// ... existing code ...

</script>

<template>
  <el-dialog
    :model-value="visible"
    title="回收站"
    width="60%"
    @close="handleClose"
  >
    <el-table v-loading="loading" :data="list" height="400">
      <el-table-column label="名称" min-width="200">
        <template #default="{ row }">
            <div class="flex items-center gap-2">
                <div class="i-ep-document text-blue-400 text-xl" />
                <span>{{ row.displayName }}</span>
            </div>
        </template>
      </el-table-column>
      <el-table-column label="删除时间" width="200">
        <template #default="{ row }">
          {{ formatDate(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleRestore(row)">还原</el-button>
          <el-button link type="danger" @click="handleDelete(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 还原目标文件夹选择弹窗 -->
    <el-dialog
      v-model="restoreDialogVisible"
      title="选择还原位置"
      width="30%"
      append-to-body
    >
      <div class="mb-4">
        <label class="block mb-2 text-sm text-gray-500">目标文件夹：</label>
        <el-tree-select
          v-model="targetFolderId"
          :data="folderTree"
          check-strictly
          :render-after-expand="false"
          placeholder="请选择目标文件夹"
          class="w-full"
          filterable
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="restoreDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="restoreLoading" @click="confirmRestore">
            确定还原
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-dialog>
</template>
