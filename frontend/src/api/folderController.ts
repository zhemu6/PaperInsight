// @ts-ignore
/* eslint-disable */
import request from "~/request";

/** 此处后端没有提供注释 POST /folder/add */
export async function addFolder(
  body: API.FolderAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/folder/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /folder/delete */
export async function deleteFolder(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/folder/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /folder/list/page */
export async function listFolderByPage(
  body: API.FolderQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFolderVO>("/folder/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /folder/update */
export async function updateFolder(
  body: API.FolderUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/folder/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
