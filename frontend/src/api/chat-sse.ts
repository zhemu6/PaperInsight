/**
 * Chat SSE Client
 * 手动实现的流式对话客户端，用于补充 OpenAPI 生成器不支持 SSE 的问题
 */

import { clearSession as apiClearSession } from './chatController'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  status: 'pending' | 'streaming' | 'done' | 'error'
  id: string
}

export interface ChatEvent {
  type: 'TEXT' | 'THINKING' | 'TOOL_USE' | 'TOOL_RESULT' | 'ERROR' | 'COMPLETE' | 'INTERRUPTED'
  seq?: number
  content?: string
  incremental?: boolean
  toolId?: string
  toolName?: string
  toolInput?: Record<string, any>
  toolResult?: string
  errorCode?: string
  error?: string
}

/**
 * 启动流式对话
 * @param chatId 对话ID (Session ID)
 * @param userQuery 用户问题
 * @param userId 用户ID
 * @param onMessage 收到消息回调 (增量文本)
 * @param onDone 完成回调
 * @param _onError 错误回调
 * @returns EventSource 实例 (用于关闭连接)
 */
export function startChatStream(
  chatId: string,
  userQuery: string,
  titleCandidate: string | undefined,
  onEvent: (event: ChatEvent) => void,
  onDone: () => void,
  onError: (error: any) => void,
): EventSource {
  // 更新为新的 API 路径: /assistant/chat
  // 参数: chatId, userQuery, userId
  const url = `/api/assistant/chat?chatId=${chatId}&userQuery=${encodeURIComponent(userQuery)}${titleCandidate ? `&title=${encodeURIComponent(titleCandidate)}` : ''}`
  const eventSource = new EventSource(url)

  let sawTerminal = false

  // Unified JSON ChatEvent stream
  eventSource.onmessage = (event) => {
    if (!event.data)
      return

    try {
      const parsed = JSON.parse(event.data) as ChatEvent
      onEvent(parsed)
      if (parsed.type === 'COMPLETE' || parsed.type === 'ERROR' || parsed.type === 'INTERRUPTED') {
        sawTerminal = true
        eventSource.close()
        onDone()
      }
    }
    catch (e) {
      console.error('Failed to parse ChatEvent', e)
    }
  }

  // 处理连接关闭/错误
  eventSource.onerror = (error) => {
    eventSource.close()
    if (!sawTerminal)
      onError(error)
  }

  return eventSource
}

/**
 * 获取历史消息
 */
export async function getChatHistory(chatId: string) {
  // 使用 fetch 直接请求，避免 request 封装带来的类型问题（除非确认后端返回 Result）
  const response = await fetch(`/api/assistant/history?chatId=${chatId}`)
  if (!response.ok) {
    throw new Error('Failed to fetch history')
  }
  const data = await response.json()
  // When backend throws BusinessException, GlobalExceptionHandler returns BaseResponse JSON
  if (data && typeof data === 'object' && !Array.isArray(data) && 'code' in data) {
    if ((data as any).code !== 0)
      throw new Error((data as any).message || 'Failed to fetch history')
    return (data as any).data
  }
  return data
}

/**
 * 清除会话上下文 (使用生成的 API)
 */
export function clearSession(paperId: number | string) {
  return apiClearSession({ paperId: Number(paperId) })
}
/**
 * 停止生成
 */
export function stopChat(chatId: string) {
  return fetch(`/api/assistant/stop?chatId=${chatId}`, {
    method: 'POST',
  })
}
