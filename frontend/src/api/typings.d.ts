declare namespace API {
  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseListPaperVO = {
    code?: number;
    data?: PaperVO[];
    message?: string;
  };

  type BaseResponseLong = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageFolderVO = {
    code?: number;
    data?: PageFolderVO;
    message?: string;
  };

  type BaseResponsePagePaperVO = {
    code?: number;
    data?: PagePaperVO;
    message?: string;
  };

  type BaseResponsePageSysUserVO = {
    code?: number;
    data?: PageSysUserVO;
    message?: string;
  };

  type BaseResponseString = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseSysUser = {
    code?: number;
    data?: SysUser;
    message?: string;
  };

  type BaseResponseSysUserVO = {
    code?: number;
    data?: SysUserVO;
    message?: string;
  };

  type DeleteRequest = {
    id: number;
  };

  type FolderAddRequest = {
    name: string;
    parentId?: number;
  };

  type FolderQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    parentId?: number;
    name?: string;
  };

  type FolderUpdateRequest = {
    id: number;
    name: string;
  };

  type FolderVO = {
    id?: number;
    name?: string;
    parentId?: number;
    userId?: number;
    createTime?: string;
    updateTime?: string;
  };

  type getUserByIdParams = {
    id: number;
  };

  type getUserVOByIdParams = {
    id: number;
  };

  type OrderItem = {
    column?: string;
    asc?: boolean;
  };

  type PageFolderVO = {
    records?: FolderVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageFolderVO;
    searchCount?: PageFolderVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PagePaperVO = {
    records?: PaperVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PagePaperVO;
    searchCount?: PagePaperVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSysUserVO = {
    records?: SysUserVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageSysUserVO;
    searchCount?: PageSysUserVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PaperAddRequest = {
    title: string;
    authors?: string;
    abstractInfo?: string;
    keywords?: string;
    cosUrl: string;
    coverUrl?: string; // Add coverUrl
    folderId?: number;
    isPublic?: number;
    publishDate?: string;
  };

  type PaperQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    folderId?: number;
    title?: string;
    keywords?: string;
    authors?: string;
    abstractInfo?: string; // Add abstractInfo
  };

  type PaperUpdateRequest = {
    id: number;
    title?: string;
    authors?: string;
    abstractInfo?: string;
    abstractInfo?: string;
    keywords?: string;
    coverUrl?: string; // Add coverUrl
    folderId?: number;
    isPublic?: number;
    publishDate?: string;
  };

  type PaperVO = {
    id?: number;
    title?: string;
    authors?: string;
    abstractInfo?: string;
    keywords?: string;
    cosUrl?: string;
    coverUrl?: string; // Add coverUrl
    folderId?: number;
    userId?: number;
    isPublic?: number;
    publishDate?: string;
    createTime?: string;
    updateTime?: string;
  };

  type RecycleRequest = {
    paperId: number;
    folderId?: number;
  };

  type sendCodeParams = {
    email: string;
  };

  type SysUser = {
    id?: number;
    userAccount?: string;
    userPassword?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    userStatus?: number;
    email?: string;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type SysUserLoginRequest = {
    userAccount?: string;
    userPassword?: string;
    email?: string;
    code?: string;
    type: string;
  };

  type SysUserPasswordResetRequest = {
    email: string;
    code: string;
    newPassword: string;
    checkPassword: string;
  };

  type SysUserQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    userName?: string;
    userAccount?: string;
    userProfile?: string;
    userRole?: string;
    userStatus?: number;
  };

  type SysUserRegisterRequest = {
    userAccount: string;
    userPassword: string;
    email: string;
    checkPassword: string;
    code: string;
  };

  type SysUserUpdateEmailRequest = {
    newEmail: string;
    code: string;
  };

  type SysUserUpdatePasswordRequest = {
    oldPassword: string;
    newPassword: string;
    checkPassword: string;
  };

  type SysUserUpdateRequest = {
    id: number;
    userName?: string;
    email?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    userStatus?: number;
  };

  type SysUserVO = {
    id?: number;
    userAccount?: string;
    userPassword?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    email?: string;
  };
}
