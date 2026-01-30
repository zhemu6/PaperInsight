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

export interface AnnouncementVO {
  id?: number
  title?: string
  content?: string
  publishTime?: string
  read?: boolean
}

export interface AnnouncementQueryRequest {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
}

export function listAnnouncementsByPage(body: AnnouncementQueryRequest) {
  return request<BaseResponse<PageResult<AnnouncementVO>>>('/announcement/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

export function getAnnouncementUnreadCount() {
  return request<BaseResponse<number>>('/announcement/unread/count', {
    method: 'GET',
  })
}

export function markAnnouncementRead(id: number) {
  return request<BaseResponse<boolean>>('/announcement/read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { id },
  })
}
