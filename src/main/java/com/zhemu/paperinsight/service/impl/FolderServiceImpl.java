package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.mapper.FolderMapper;
import com.zhemu.paperinsight.model.dto.folder.FolderAddRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderQueryRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderUpdateRequest;
import com.zhemu.paperinsight.model.entity.Folder;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.vo.FolderVO;
import com.zhemu.paperinsight.service.FolderService;
import com.zhemu.paperinsight.service.PaperInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件夹服务实现类
 * 
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder> implements FolderService {

    private final PaperInfoService paperInfoService;

    @Override
    public long addFolder(FolderAddRequest folderAddRequest) {
        String name = folderAddRequest.getName();
        Long parentId = folderAddRequest.getParentId();
        // 1. 判断是否是根目录
        if (parentId == null) {
            parentId = 0L;
        }

        // 2. 校验同级目录下是否存在重名文件夹
        QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("name", name);
        // 多租户插件会自动追加 user_id = xxx
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "同级目录下文件夹名称重复");

        // 3. 保存文件夹
        Folder folder = Folder.builder().name(name).parentId(parentId).build();
        boolean result = this.save(folder);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "请稍后重试");
        return folder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFolder(long id) {
        // 1. 检查文件夹是否存在
        Folder folder = this.getById(id);
        // 如果已经删除了（或找不到），直接返回true（幂等）
        if (folder == null) {
            return true;
        }

        // 2. 检查是否有子内容 (子文件夹/论文)
        QueryWrapper<Folder> subFolderQuery = new QueryWrapper<>();
        subFolderQuery.eq("parent_id", id);
        long subFolderCount = this.count(subFolderQuery);

        QueryWrapper<PaperInfo> paperQuery = new QueryWrapper<>();
        paperQuery.eq("folder_id", id);
        long paperCount = paperInfoService.count(paperQuery);

        if (subFolderCount > 0 || paperCount > 0) {
            // 递归删除逻辑 (逻辑删除)
            // A. 删除所有子文件夹
            if (subFolderCount > 0) {
                List<Folder> subFolders = this.list(subFolderQuery);
                for (Folder sub : subFolders) {
                    // 递归调用自身
                    this.deleteFolder(sub.getId());
                }
            }
            // B. 删除该目录下的所有论文
            if (paperCount > 0) {
                paperInfoService.remove(paperQuery);
            }
        }

        // 3. 删除自己
        return this.removeById(id);
    }

    @Override
    public boolean updateFolder(FolderUpdateRequest folderUpdateRequest) {
        Long id = folderUpdateRequest.getId();
        String name = folderUpdateRequest.getName();
        // 1. 查询待更新文件夹
        Folder folder = this.getById(id);
        ThrowUtils.throwIf(folder == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 校验重名 (排除自己)
        QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", folder.getParentId());
        queryWrapper.eq("name", name);
        queryWrapper.ne("id", id);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "名称重复");

        folder.setName(name);
        return this.updateById(folder);
    }

    @Override
    public Page<FolderVO> listFolderVOByPage(FolderQueryRequest folderQueryRequest) {
        long pageNum = folderQueryRequest.getPageNum();
        long pageSize = folderQueryRequest.getPageSize();
        QueryWrapper<Folder> queryWrapper = this.getQueryWrapper(folderQueryRequest);
        Page<Folder> folderPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        Page<FolderVO> folderVOPage = new Page<>(pageNum, pageSize, folderPage.getTotal());
        List<FolderVO> folderVOList = folderPage.getRecords().stream()
                .map(FolderVO::objToVo)
                .collect(Collectors.toList());
        folderVOPage.setRecords(folderVOList);
        return folderVOPage;
    }

    /**
     * 获取查询包装类
     *
     * @param folderQueryRequest
     * @return
     */
    public QueryWrapper<Folder> getQueryWrapper(FolderQueryRequest folderQueryRequest) {
        ThrowUtils.throwIf(folderQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long id = folderQueryRequest.getId();
        Long parentId = folderQueryRequest.getParentId();
        String name = folderQueryRequest.getName();
        String sortField = folderQueryRequest.getSortField();
        String sortOrder = folderQueryRequest.getSortOrder();

        QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(parentId != null, "parent_id", parentId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

}
