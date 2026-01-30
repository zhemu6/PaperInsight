import request from '~/request'

export interface PageResult<T> {
  records?: T[]
  total?: number
  size?: number
  current?: number
  pages?: number
}

export interface BaseResponse<T> {
  code?: number
  data?: T
  message?: string
}

export interface NotificationVO {
  id?: number
  type?: string
  title?: string
  content?: string
  payloadJson?: string
  readTime?: string
  createTime?: string
}

export interface NotificationQueryRequest {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  unreadOnly?: boolean
}

export function listNotificationsByPage(body: NotificationQueryRequest) {
  return request<BaseResponse<PageResult<NotificationVO>>>('/notification/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

export function getNotificationUnreadCount() {
  return request<BaseResponse<number>>('/notification/unread/count', {
    method: 'GET',
  })
}

export function markNotificationRead(id: number) {
  return request<BaseResponse<boolean>>('/notification/read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { id },
  })
}

export function markNotificationReadBatch(ids: number[]) {
  return request<BaseResponse<boolean>>('/notification/read/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { ids },
  })
}

export function markNotificationAllRead() {
  return request<BaseResponse<boolean>>('/notification/read/all', {
    method: 'POST',
  })
}
