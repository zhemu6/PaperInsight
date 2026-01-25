// @ts-ignore
/* eslint-disable */
import request from "~/request";

/** 此处后端没有提供注释 POST /recycle/delete */
export async function physicalDelete(
  body: API.RecycleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/recycle/delete", {
    method: "POST",
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /recycle/list */
export async function listRecycleBin(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPaperVO>("/recycle/list", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /recycle/restore */
export async function restore(
  body: API.RecycleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/recycle/restore", {
    method: "POST",
    data: body,
    ...(options || {}),
  });
}
