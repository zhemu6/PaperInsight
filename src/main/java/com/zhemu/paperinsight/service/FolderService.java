package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhemu.paperinsight.model.dto.folder.FolderAddRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderQueryRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderUpdateRequest;
import com.zhemu.paperinsight.model.entity.Folder;
import com.zhemu.paperinsight.model.vo.FolderVO;

/**
 * 文件夹服务接口
 * @author lushihao
 */
public interface FolderService extends IService<Folder> {

    /**
     * 创建文件夹
     *
     * @param folderAddRequest
     * @return
     */
    long addFolder(FolderAddRequest folderAddRequest);

    /**
     * 删除文件夹 (需处理递归逻辑)
     *
     * @param id        文件夹ID
     * @return
     */
    boolean deleteFolder(long id);

    /**
     * 更新文件夹
     *
     * @param folderUpdateRequest
     * @return
     */
    boolean updateFolder(FolderUpdateRequest folderUpdateRequest);

    /**
     * 分页获取文件夹列表
     *
     * @param folderQueryRequest
     * @return
     */
    Page<FolderVO> listFolderVOByPage(FolderQueryRequest folderQueryRequest);

    QueryWrapper<Folder> getQueryWrapper(FolderQueryRequest folderQueryRequest);

}
