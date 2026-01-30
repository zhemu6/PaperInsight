import request from '~/request'

export interface ChatSessionVO {
  chatId: string
  paperId: number
  title?: string
  lastMessageAt?: string
  createTime?: string
}

export function listChatSessions(paperId: number) {
  return request<any>('/assistant/session/list', {
    method: 'GET',
    params: { paperId },
  })
}

export function createChatSession(paperId: number) {
  return request<any>('/assistant/session/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { paperId },
  })
}

export function deleteChatSession(chatId: string) {
  return request<any>('/assistant/session/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { chatId },
  })
}
