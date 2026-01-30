<script setup lang="ts">
import type { AnnouncementAdminVO } from '~/api/admin-announcement'
import type { AnnouncementVO } from '~/api/announcement'
import type { NotificationVO } from '~/api/notification'
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  adminCreateAnnouncement,
  adminDeleteAnnouncement,
  adminListAnnouncementsByPage,
  adminPublishAnnouncement,
  adminUpdateAnnouncement,
} from '~/api/admin-announcement'
import {
  getAnnouncementUnreadCount,
  listAnnouncementsByPage,
  markAnnouncementRead,
} from '~/api/announcement'
import {
  getNotificationUnreadCount,
  listNotificationsByPage,
  markNotificationAllRead,
  markNotificationRead,
} from '~/api/notification'
import { useUserStore } from '~/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
const router = useRouter()

const isAdmin = computed(() => userStore.loginUser?.userRole === 'admin')

const activeTab = ref<'notifications' | 'announcements'>('notifications')

// Polling unread counts
const notificationUnread = ref(0)
const announcementUnread = ref(0)
let pollTimer: number | undefined

async function refreshUnreadCounts() {
  try {
    const [nRes, aRes] = await Promise.all([
      getNotificationUnreadCount(),
      getAnnouncementUnreadCount(),
    ])
    notificationUnread.value = nRes.data || 0
    announcementUnread.value = aRes.data || 0
  }
  catch {
    // silent
  }
}

// Notifications list
const notificationLoading = ref(false)
const notificationList = ref<NotificationVO[]>([])
const notificationTotal = ref(0)
const notificationPageNum = ref(1)
const notificationPageSize = ref(10)
const notificationUnreadOnly = ref(false)

async function fetchNotifications() {
  try {
    notificationLoading.value = true
    const res = await listNotificationsByPage({
      pageNum: notificationPageNum.value,
      pageSize: notificationPageSize.value,
      unreadOnly: notificationUnreadOnly.value,
    })
    notificationList.value = res.data?.records || []
    notificationTotal.value = res.data?.total || 0
  }
  finally {
    notificationLoading.value = false
  }
}

async function handleReadAllNotifications() {
  await markNotificationAllRead()
  await Promise.all([fetchNotifications(), refreshUnreadCounts()])
  ElMessage.success(t('notification.markAllReadSuccess'))
}

async function handleReadNotification(item: NotificationVO) {
  if (!item.id)
    return
  await markNotificationRead(item.id)
  await Promise.all([fetchNotifications(), refreshUnreadCounts()])
}

function parsePayload(payloadJson?: string) {
  if (!payloadJson)
    return null
  try {
    return JSON.parse(payloadJson) as Record<string, unknown>
  }
  catch {
    return null
  }
}

function openPaperFromNotification(item: NotificationVO) {
  const payload = parsePayload(item.payloadJson)
  const paperId = payload?.paperId
  if (typeof paperId !== 'number') {
    ElMessage.warning(t('notification.paperNotFound'))
    return
  }
  router.push(`/paper/detail/${paperId}`)
}

// Announcements list
const announcementLoading = ref(false)
const announcementList = ref<AnnouncementVO[]>([])
const announcementTotal = ref(0)
const announcementPageNum = ref(1)
const announcementPageSize = ref(10)

async function fetchAnnouncements() {
  try {
    announcementLoading.value = true
    const res = await listAnnouncementsByPage({
      pageNum: announcementPageNum.value,
      pageSize: announcementPageSize.value,
    })
    announcementList.value = res.data?.records || []
    announcementTotal.value = res.data?.total || 0
  }
  finally {
    announcementLoading.value = false
  }
}

async function handleReadAnnouncement(item: AnnouncementVO) {
  if (!item.id)
    return
  await markAnnouncementRead(item.id)
  await Promise.all([fetchAnnouncements(), refreshUnreadCounts()])
}

// Admin announcement management
const adminVisible = ref(false)
const adminLoading = ref(false)
const adminList = ref<AnnouncementAdminVO[]>([])
const adminTotal = ref(0)
const adminPageNum = ref(1)
const adminPageSize = ref(10)
const adminStatus = ref<string>('')

const editorVisible = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const editorForm = ref<{ id?: number, title: string, content: string }>({
  title: '',
  content: '',
})

async function fetchAdminAnnouncements() {
  if (!isAdmin.value)
    return
  try {
    adminLoading.value = true
    const res = await adminListAnnouncementsByPage({
      pageNum: adminPageNum.value,
      pageSize: adminPageSize.value,
      status: adminStatus.value || undefined,
    })
    adminList.value = res.data?.records || []
    adminTotal.value = res.data?.total || 0
  }
  finally {
    adminLoading.value = false
  }
}

function openCreate() {
  editorMode.value = 'create'
  editorForm.value = { title: '', content: '' }
  editorVisible.value = true
}

function openEdit(row: AnnouncementAdminVO) {
  editorMode.value = 'edit'
  editorForm.value = {
    id: row.id,
    title: row.title || '',
    content: row.content || '',
  }
  editorVisible.value = true
}

async function submitEditor() {
  const title = editorForm.value.title.trim()
  const content = editorForm.value.content.trim()
  if (!title || !content)
    return ElMessage.warning(t('notification.admin.inputRequired'))

  if (editorMode.value === 'create') {
    await adminCreateAnnouncement({ title, content })
    ElMessage.success(t('notification.admin.createSuccess'))
  }
  else {
    await adminUpdateAnnouncement({
      id: editorForm.value.id as number,
      title,
      content,
    })
    ElMessage.success(t('notification.admin.updateSuccess'))
  }
  editorVisible.value = false
  await fetchAdminAnnouncements()
  await fetchAnnouncements()
}

async function togglePublish(row: AnnouncementAdminVO) {
  if (!row.id)
    return
  const published = row.status !== 'published'
  await adminPublishAnnouncement(row.id, published)
  ElMessage.success(published ? t('notification.admin.publishSuccess') : t('notification.admin.unpublishSuccess'))
  await Promise.all([fetchAdminAnnouncements(), fetchAnnouncements(), refreshUnreadCounts()])
}

async function deleteAnnouncement(row: AnnouncementAdminVO) {
  if (!row.id)
    return
  await adminDeleteAnnouncement(row.id)
  ElMessage.success(t('notification.admin.deleteSuccess'))
  await Promise.all([fetchAdminAnnouncements(), fetchAnnouncements(), refreshUnreadCounts()])
}

async function init() {
  await Promise.all([
    refreshUnreadCounts(),
    fetchNotifications(),
    fetchAnnouncements(),
  ])
}

onMounted(async () => {
  await init()
  pollTimer = window.setInterval(refreshUnreadCounts, 15000)
})

onUnmounted(() => {
  if (pollTimer)
    window.clearInterval(pollTimer)
})

const notificationsTabLabel = computed(() => t('notification.tabs.notifications'))
const announcementsTabLabel = computed(() => t('notification.tabs.announcements'))
</script>

<template>
  <div class="min-h-[calc(100vh-60px)]">
    <div class="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100">
          {{ t('notification.title') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ t('notification.subtitle') }}
        </p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="notification-tabs">
      <el-tab-pane name="notifications">
        <template #label>
          <el-badge :value="notificationUnread" :hidden="notificationUnread === 0" class="mr-2">
            <span>{{ notificationsTabLabel }}</span>
          </el-badge>
        </template>

        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <el-checkbox v-model="notificationUnreadOnly" @change="fetchNotifications">
            {{ t('notification.unreadOnly') }}
          </el-checkbox>
          <div class="flex items-center gap-2">
            <el-button size="small" @click="fetchNotifications">
              {{ t('common.refresh') }}
            </el-button>
            <el-button size="small" type="primary" @click="handleReadAllNotifications">
              {{ t('notification.markAllRead') }}
            </el-button>
          </div>
        </div>

        <el-skeleton v-if="notificationLoading" :rows="5" animated />
        <div v-else class="space-y-3">
          <div v-if="notificationList.length === 0" class="rounded-lg border border-dashed border-gray-200 p-8 text-center text-gray-500">
            {{ t('common.noData') }}
          </div>
          <div
            v-for="item in notificationList"
            :key="item.id"
            class="group rounded-xl border border-gray-100 bg-white p-4 shadow-sm transition hover:border-gray-200 dark:border-gray-800 dark:bg-gray-900"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">{{ item.title }}</span>
                  <el-tag v-if="!item.readTime" size="small" type="warning">{{ t('notification.unread') }}</el-tag>
                </div>
                <div class="mt-2 whitespace-pre-wrap text-sm text-gray-600 dark:text-gray-300">{{ item.content }}</div>
                <div class="mt-3 text-xs text-gray-400">{{ item.createTime }}</div>
              </div>
              <div class="flex flex-shrink-0 items-center gap-2">
                <el-button
                  v-if="parsePayload(item.payloadJson)?.paperId"
                  size="small"
                  type="primary"
                  plain
                  @click="openPaperFromNotification(item)"
                >
                  {{ t('notification.openPaper') }}
                </el-button>
                <el-button
                  v-if="!item.readTime"
                  size="small"
                  @click="handleReadNotification(item)"
                >
                  {{ t('notification.markRead') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="notificationPageNum"
            v-model:page-size="notificationPageSize"
            :total="notificationTotal"
            layout="prev, pager, next, sizes"
            @current-change="fetchNotifications"
            @size-change="fetchNotifications"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane name="announcements">
        <template #label>
          <el-badge :value="announcementUnread" :hidden="announcementUnread === 0" class="mr-2">
            <span>{{ announcementsTabLabel }}</span>
          </el-badge>
        </template>

        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div class="flex items-center gap-2">
            <el-button size="small" @click="fetchAnnouncements">
              {{ t('common.refresh') }}
            </el-button>
            <el-button v-if="isAdmin" size="small" type="primary" @click="adminVisible = !adminVisible; fetchAdminAnnouncements()">
              {{ adminVisible ? t('notification.admin.hideManage') : t('notification.admin.showManage') }}
            </el-button>
          </div>
        </div>

        <el-skeleton v-if="announcementLoading" :rows="5" animated />
        <div v-else class="space-y-3">
          <div v-if="announcementList.length === 0" class="rounded-lg border border-dashed border-gray-200 p-8 text-center text-gray-500">
            {{ t('common.noData') }}
          </div>
          <div
            v-for="item in announcementList"
            :key="item.id"
            class="rounded-xl border border-gray-100 bg-white p-4 shadow-sm dark:border-gray-800 dark:bg-gray-900"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">{{ item.title }}</span>
                  <el-tag v-if="!item.read" size="small" type="warning">{{ t('notification.unread') }}</el-tag>
                </div>
                <div class="mt-2 whitespace-pre-wrap text-sm text-gray-600 dark:text-gray-300">{{ item.content }}</div>
                <div class="mt-3 text-xs text-gray-400">{{ item.publishTime }}</div>
              </div>
              <div class="flex flex-shrink-0 items-center gap-2">
                <el-button
                  v-if="!item.read"
                  size="small"
                  @click="handleReadAnnouncement(item)"
                >
                  {{ t('notification.markRead') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="announcementPageNum"
            v-model:page-size="announcementPageSize"
            :total="announcementTotal"
            layout="prev, pager, next, sizes"
            @current-change="fetchAnnouncements"
            @size-change="fetchAnnouncements"
          />
        </div>

        <el-drawer v-model="adminVisible" :title="t('notification.admin.title')" size="720px">
          <div class="mb-4 flex items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <el-select v-model="adminStatus" size="small" class="w-[160px]" @change="fetchAdminAnnouncements">
                <el-option :label="t('notification.admin.all')" value="" />
                <el-option :label="t('notification.admin.draft')" value="draft" />
                <el-option :label="t('notification.admin.statusPublished')" value="published" />
              </el-select>
              <el-button size="small" @click="fetchAdminAnnouncements">
                {{ t('common.refresh') }}
              </el-button>
            </div>
            <el-button size="small" type="primary" @click="openCreate">
              {{ t('notification.admin.create') }}
            </el-button>
          </div>

          <el-table v-loading="adminLoading" :data="adminList" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" :label="t('notification.admin.table.title')" min-width="180" />
            <el-table-column prop="status" :label="t('notification.admin.table.status')" width="120" />
            <el-table-column prop="publishTime" :label="t('notification.admin.table.publishTime')" width="180" />
            <el-table-column :label="t('notification.admin.table.actions')" width="240">
              <template #default="scope">
                <el-button size="small" @click="openEdit(scope.row)">
                  {{ t('common.edit') }}
                </el-button>
                <el-button size="small" type="primary" @click="togglePublish(scope.row)">
                  {{ scope.row.status === 'published' ? t('notification.admin.unpublish') : t('notification.admin.publish') }}
                </el-button>
                <el-button size="small" type="danger" @click="deleteAnnouncement(scope.row)">
                  {{ t('common.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="adminPageNum"
              v-model:page-size="adminPageSize"
              :total="adminTotal"
              layout="prev, pager, next, sizes"
              @current-change="fetchAdminAnnouncements"
              @size-change="fetchAdminAnnouncements"
            />
          </div>
        </el-drawer>

        <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? t('notification.admin.create') : t('notification.admin.edit')" width="720px">
          <el-form label-position="top">
            <el-form-item :label="t('notification.admin.form.title')">
              <el-input v-model="editorForm.title" />
            </el-form-item>
            <el-form-item :label="t('notification.admin.form.content')">
              <el-input v-model="editorForm.content" type="textarea" :autosize="{ minRows: 8, maxRows: 18 }" />
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="flex justify-end gap-2">
              <el-button @click="editorVisible = false">{{ t('common.cancel') }}</el-button>
              <el-button type="primary" @click="submitEditor">{{ t('common.submit') }}</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<route lang="yaml">
meta:
  layout: BasicLayout
</route>

<style scoped>
.notification-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
</style>
