declare namespace API {
  interface BaseResponseBoolean {
    code?: number
    data?: boolean
    message?: string
  }

  interface BaseResponseListPaperVO {
    code?: number
    data?: PaperVO[]
    message?: string
  }

  interface BaseResponseLong {
    code?: number
    data?: number
    message?: string
  }

  interface BaseResponseMapStringLong {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  interface BaseResponsePageFolderVO {
    code?: number
    data?: PageFolderVO
    message?: string
  }

  interface BaseResponsePagePaperVO {
    code?: number
    data?: PagePaperVO
    message?: string
  }

  interface BaseResponsePageSysUserVO {
    code?: number
    data?: PageSysUserVO
    message?: string
  }

  interface BaseResponsePaperDetailVO {
    code?: number
    data?: PaperDetailVO
    message?: string
  }

  interface BaseResponseString {
    code?: number
    data?: string
    message?: string
  }

  interface BaseResponseSysUser {
    code?: number
    data?: SysUser
    message?: string
  }

  interface BaseResponseSysUserVO {
    code?: number
    data?: SysUserVO
    message?: string
  }

  interface chatStreamParams {
    paperId: number
    question: string
  }

  interface clearSessionParams {
    paperId: number
  }

  interface DeleteRequest {
    id: number
  }

  interface FolderAddRequest {
    name: string
    parentId?: number
  }

  interface FolderQueryRequest {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    parentId?: number
    name?: string
  }

  interface FolderUpdateRequest {
    id: number
    name: string
  }

  interface FolderVO {
    id?: number
    name?: string
    parentId?: number
    userId?: number
    createTime?: string
    updateTime?: string
  }

  interface getPaperDetailParams {
    id: number
  }

  interface getUserByIdParams {
    id: number
  }

  interface getUserVOByIdParams {
    id: number
  }

  interface OrderItem {
    column?: string
    asc?: boolean
  }

  interface PageFolderVO {
    records?: FolderVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageFolderVO
    searchCount?: PageFolderVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  interface PagePaperVO {
    records?: PaperVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePaperVO
    searchCount?: PagePaperVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  interface PageSysUserVO {
    records?: SysUserVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageSysUserVO
    searchCount?: PageSysUserVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  interface PaperAddRequest {
    title: string
    authors?: string
    abstractInfo?: string
    keywords?: string
    cosUrl?: string
    coverUrl?: string
    folderId?: number
    isPublic?: number
    publishDate?: string
  }

  interface PaperDetailVO {
    paperInfo?: PaperVO
    paperInsight?: PaperInsightVO
  }

  interface PaperInsightVO {
    summaryMarkdown?: string
    innovationPoints?: string
    methods?: string
    score?: number
    scoreDetails?: Record<string, any>
  }

  interface PaperQueryRequest {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    folderId?: number
    title?: string
    keywords?: string
    authors?: string
    abstractInfo?: string
  }

  interface PaperUpdateRequest {
    id: number
    title?: string
    authors?: string
    abstractInfo?: string
    coverUrl?: string
    folderId?: number
    isPublic?: number
    publishDate?: string
  }

  interface PaperVO {
    id?: number
    title?: string
    authors?: string
    abstractInfo?: string
    keywords?: string
    cosUrl?: string
    coverUrl?: string
    folderId?: number
    userId?: number
    isPublic?: number
    publishDate?: string
    createTime?: string
    updateTime?: string
  }

  interface RecycleRequest {
    paperId: number
    folderId?: number
  }

  interface sendCodeParams {
    email: string
  }

  type ServerSentEventString = true

  interface SysUser {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userStatus?: number
    email?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  interface SysUserLoginRequest {
    userAccount?: string
    userPassword?: string
    email?: string
    code?: string
    type: string
  }

  interface SysUserPasswordResetRequest {
    email: string
    code: string
    newPassword: string
    checkPassword: string
  }

  interface SysUserQueryRequest {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
    userStatus?: number
  }

  interface SysUserRegisterRequest {
    userAccount: string
    userPassword: string
    email: string
    checkPassword: string
    code: string
  }

  interface SysUserUpdateEmailRequest {
    newEmail: string
    code: string
  }

  interface SysUserUpdatePasswordRequest {
    oldPassword: string
    newPassword: string
    checkPassword: string
  }

  interface SysUserUpdateRequest {
    id: number
    userName?: string
    email?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userStatus?: number
  }

  interface SysUserVO {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    email?: string
  }
}
