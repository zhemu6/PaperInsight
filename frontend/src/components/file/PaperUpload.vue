<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { uploadFile } from '~/api/fileController'
import { addPaper } from '~/api/paperController'

const props = defineProps<{
  visible: boolean
  folderId: number
}>()

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()

// Set worker source
// 使用 CDN 或者本地静态资源配置 worker
// 注意：在 Vite 中，通常需要配置 worker
pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs'

const activeStep = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)

// Step 1: File Upload
const uploadedFileUrl = ref('')
const uploadedFileName = ref('')
const uploadProgress = ref(0)

// Step 2: Metadata Form
const formRef = ref()
const formData = reactive({
  title: '',
  authors: '',
  abstractInfo: '',
  keywords: '',
  isPublic: 0,
  cosUrl: '',
  coverUrl: '',
  folderId: 0,
})

const keywordsList = ref<string[]>([])
const keywordInput = ref('')

function handleKeywordInputConfirm() {
  if (keywordInput.value) {
    if (!keywordsList.value.includes(keywordInput.value)) {
      keywordsList.value.push(keywordInput.value)
    }
    keywordInput.value = ''
  }
}

function handleKeywordClose(tag: string) {
  keywordsList.value.splice(keywordsList.value.indexOf(tag), 1)
}

watch(keywordsList, (val) => {
  formData.keywords = val.join(' ')
}, { deep: true })

const rules = {
  title: [{ required: true, message: t('file.upload.messages.inputTitle'), trigger: 'blur' }],
  cosUrl: [{ required: true, message: t('file.upload.messages.uploadFirst'), trigger: 'change' }],
}

function reset() {
  activeStep.value = 0
  uploadedFileUrl.value = ''
  uploadedFileName.value = ''
  uploadProgress.value = 0
  formData.title = ''
  formData.authors = ''
  formData.abstractInfo = ''
  formData.keywords = ''
  formData.isPublic = 0
  formData.cosUrl = ''
  formData.coverUrl = ''
}

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    reset()
  }
})

// 生成封面
async function generateCover(file: File): Promise<string> {
  try {
    const arrayBuffer = await file.arrayBuffer()
    const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer })
    const pdf = await loadingTask.promise
    const page = await pdf.getPage(1)

    const scale = 1.0
    const viewport = page.getViewport({ scale })

    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')
    if (!context)
      return ''

    canvas.height = viewport.height
    canvas.width = viewport.width

    // Draw white background
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)

    await page.render({
      canvasContext: context,
      viewport,
    }).promise

    // Convert to blob and upload
    return new Promise((resolve) => {
      canvas.toBlob(async (blob) => {
        if (blob) {
          // console.log('Blob created, size:', blob.size)
          const uploadData = new FormData()
          uploadData.append('file', blob, 'cover.png')
          try {
            const res = await uploadFile(uploadData) as any
            if (res.code === 0 && res.data) {
              resolve(res.data)
            }
            else {
              resolve('')
            }
          }
          catch (uploadErr) {
            console.error('Cover upload request failed:', uploadErr)
            resolve('')
          }
        }
        else {
          console.error('Blob creation failed')
          resolve('')
        }
      }, 'image/png')
    })
  }
  catch (e) {
    console.error('Cover generation failed', e)
    return ''
  }
}

// Custom Upload Handler
async function handleUpload(options: UploadRequestOptions) {
  const { file, onProgress, onSuccess, onError } = options

  try {
    loading.value = true
    // Simulate progress
    const interval = setInterval(() => {
      if (uploadProgress.value < 80) {
        uploadProgress.value += 10
        onProgress({ percent: uploadProgress.value } as any)
      }
    }, 200)

    // 1. Upload PDF
    const uploadData = new FormData()
    uploadData.append('file', file)
    const res = await uploadFile(uploadData) as any

    // 2. Generate Cover (Parallel or Sequential, let's do parallel but wait for both if possible, or just sequential for simplicity)
    // Actually sequentially is safer to update progress correctly

    if (res.code === 0 && res.data) {
      uploadedFileUrl.value = res.data
      formData.cosUrl = res.data

      // 3. Generate Cover
      const cover = await generateCover(file as File)
      formData.coverUrl = cover

      clearInterval(interval)
      uploadProgress.value = 100
      onProgress({ percent: 100 } as any)

      uploadedFileName.value = file.name
      // Auto-fill title with filename (without extension)
      const nameWithoutExt = file.name.substring(0, file.name.lastIndexOf('.')) || file.name
      formData.title = nameWithoutExt

      ElMessage.success(t('file.upload.messages.success'))
      onSuccess(res.data)
      // Auto advance to next step
      setTimeout(() => {
        activeStep.value = 1
      }, 500)
    }
    else {
      throw new Error(res.message || t('file.upload.messages.uploadFailed'))
    }
  }
  catch (error: any) {
    ElMessage.error(error.message || t('file.upload.messages.uploadFailed'))
    onError(error)
  }
  finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value)
    return

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        formData.folderId = props.folderId
        await addPaper(formData)
        ElMessage.success(t('file.upload.messages.addSuccess'))
        dialogVisible.value = false
        emits('success')
      }
      catch (error: any) {
        // Error handled in request interceptor usually, but safe to catch
      }
      finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="t('file.upload.title')"
    width="600px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div class="px-4">
      <el-steps :active="activeStep" finish-status="success" class="mb-8" simple>
        <el-step :title="t('file.upload.steps.upload')" icon="i-ep-upload" />
        <el-step :title="t('file.upload.steps.info')" icon="i-ep-edit" />
      </el-steps>

      <!-- Step 1: Upload -->
      <div v-if="activeStep === 0" class="flex flex-col items-center justify-center py-8">
        <el-upload
          class="w-full"
          drag
          action="#"
          :http-request="handleUpload"
          :show-file-list="false"
          accept=".pdf,.doc,.docx,.ppt,.pptx"
        >
          <div class="i-ep-upload-filled mx-auto mb-4 text-6xl text-gray-400" />
          <div class="el-upload__text">
            {{ t('file.upload.drag') }} <em>{{ t('file.upload.click') }}</em>
          </div>
          <template #tip>
            <div class="el-upload__tip text-center">
              {{ t('file.upload.tip') }}
            </div>
          </template>
        </el-upload>
      </div>

      <!-- Step 2: Form -->
      <div v-else class="py-2">
        <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
          <el-form-item :label="t('file.upload.form.file')">
            <div class="w-full flex items-center gap-2 border border-gray-200 rounded bg-gray-50 px-3 py-1 text-gray-500">
              <div class="i-ep-document" />
              <span class="flex-1 truncate">{{ uploadedFileName }}</span>
              <div class="i-ep-success-filled text-green-500" />
            </div>
            <div v-if="formData.coverUrl" class="mt-2 h-40 w-32 overflow-hidden border border-gray-200 rounded">
              <img :src="formData.coverUrl" class="h-full w-full bg-white object-cover" alt="Cover">
            </div>
          </el-form-item>

          <el-form-item :label="t('file.upload.form.title')" prop="title">
            <el-input v-model="formData.title" :placeholder="t('file.upload.form.titlePlaceholder')" />
          </el-form-item>

          <el-form-item :label="t('file.upload.form.authors')" prop="authors">
            <el-input v-model="formData.authors" :placeholder="t('file.upload.form.authorsPlaceholder')" />
          </el-form-item>

          <el-form-item :label="t('file.upload.form.keywords')" prop="keywords">
            <div class="w-full flex flex-wrap gap-2">
              <el-tag
                v-for="tag in keywordsList"
                :key="tag"
                closable
                :disable-transitions="false"
                @close="handleKeywordClose(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="keywordsList.length < 5"
                v-model="keywordInput"
                class="!w-32"
                size="small"
                :placeholder="t('file.upload.form.keywordsPlaceholder')"
                @keyup.enter="handleKeywordInputConfirm"
                @blur="handleKeywordInputConfirm"
              />
            </div>
            <!-- Hidden input for binding -->
            <el-input v-model="formData.keywords" type="hidden" />
          </el-form-item>

          <el-form-item :label="t('file.upload.form.abstract')" prop="abstractInfo">
            <el-input
              v-model="formData.abstractInfo"
              type="textarea"
              :rows="4"
              :placeholder="t('file.upload.form.abstractPlaceholder')"
            />
          </el-form-item>

          <el-form-item :label="t('file.upload.form.public')">
            <el-switch
              v-model="formData.isPublic"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('file.upload.form.publicYes')"
              :inactive-text="t('file.upload.form.publicNo')"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button v-if="activeStep === 1" @click="activeStep = 0">{{ t('file.upload.form.prev') }}</el-button>
        <el-button @click="dialogVisible = false">{{ t('file.upload.form.cancel') }}</el-button>
        <el-button
          v-if="activeStep === 1"
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ t('file.upload.form.submit') }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
:deep(.el-step__icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
