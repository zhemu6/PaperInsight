// @ts-ignore
/* eslint-disable */
import request from "~/request";

/** 此处后端没有提供注释 POST /paper/add */
export async function addPaper(
  body: API.PaperAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/paper/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /paper/delete */
export async function deletePaper(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/paper/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /paper/get */
export async function getPaperDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPaperDetailParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaperDetailVO>("/paper/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /paper/list/page */
export async function listPaperByPage(
  body: API.PaperQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePaperVO>("/paper/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /paper/list/public/page */
export async function listPublicPaperByPage(
  body: API.PaperQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePaperVO>("/paper/list/public/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /paper/update */
export async function updatePaper(
  body: API.PaperUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/paper/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
