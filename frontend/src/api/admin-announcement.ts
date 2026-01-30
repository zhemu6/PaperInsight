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

export interface AnnouncementAdminVO {
  id?: number
  title?: string
  content?: string
  status?: string
  publishTime?: string
  publisherId?: number
  createTime?: string
  updateTime?: string
}

export interface AnnouncementAdminQueryRequest {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  status?: string
}

export function adminListAnnouncementsByPage(body: AnnouncementAdminQueryRequest) {
  return request<BaseResponse<PageResult<AnnouncementAdminVO>>>('/admin/announcement/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

export function adminCreateAnnouncement(body: { title: string, content: string }) {
  return request<BaseResponse<number>>('/admin/announcement/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

export function adminUpdateAnnouncement(body: { id: number, title: string, content: string }) {
  return request<BaseResponse<boolean>>('/admin/announcement/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

export function adminDeleteAnnouncement(id: number) {
  return request<BaseResponse<boolean>>('/admin/announcement/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { id },
  })
}

export function adminPublishAnnouncement(id: number, published: boolean) {
  return request<BaseResponse<boolean>>('/admin/announcement/publish', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { id, published },
  })
}
