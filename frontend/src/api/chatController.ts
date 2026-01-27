// @ts-ignore
/* eslint-disable */
import request from "~/request";

/** 此处后端没有提供注释 DELETE /api/chat/session */
export async function clearSession(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.clearSessionParams,
  options?: { [key: string]: any }
) {
  return request<boolean>("/api/chat/session", {
    method: "DELETE",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/chat/stream */
export async function chatStream(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatStreamParams,
  options?: { [key: string]: any }
) {
  return request<API.ServerSentEventString[]>("/api/chat/stream", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
