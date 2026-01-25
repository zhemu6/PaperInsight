package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.paper.PaperAddRequest;
import com.zhemu.paperinsight.model.dto.paper.PaperQueryRequest;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.vo.PaperVO;
import com.zhemu.paperinsight.service.PaperInfoService;
import com.zhemu.paperinsight.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lushihao
 * @description 针对表【paper_info(论文信息表)】的数据库操作Service实现
 * @createDate 2026-01-23 23:24:48
 */
@Service
@RequiredArgsConstructor
public class PaperInfoServiceImpl extends ServiceImpl<PaperInfoMapper, PaperInfo>
        implements PaperInfoService {
    /**
     * 添加论文请求
     * 
     * @param paperAddRequest 论文添加请求
     * @return 是否添加成功
     */
    @Override
    public Long addPaper(PaperAddRequest paperAddRequest) {
        PaperInfo paperInfo = new PaperInfo();
        BeanUtils.copyProperties(paperAddRequest, paperInfo);
        // 用户Id 插件会自动传入
        boolean result = this.save(paperInfo);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return paperInfo.getId();
    }

    @Override
    public QueryWrapper<PaperInfo> getQueryWrapper(PaperQueryRequest paperQueryRequest) {
        ThrowUtils.throwIf(paperQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = paperQueryRequest.getId();
        Long folderId = paperQueryRequest.getFolderId();
        String title = paperQueryRequest.getTitle();
        String keywords = paperQueryRequest.getKeywords();
        String authors = paperQueryRequest.getAuthors();
        String abstractInfo = paperQueryRequest.getAbstractInfo();
        String sortField = paperQueryRequest.getSortField();
        String sortOrder = paperQueryRequest.getSortOrder();

        QueryWrapper<PaperInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(folderId != null, "folder_id", folderId);
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(keywords), "keywords", keywords);
        queryWrapper.like(StrUtil.isNotBlank(authors), "authors", authors);
        queryWrapper.like(StrUtil.isNotBlank(abstractInfo), "abstract_info", abstractInfo);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);

        return queryWrapper;
    }

    /**
     * 查看已删除
     *
     * @param userId 用户 id
     * @return 删除论文列表
     */
    @Override
    public List<PaperVO> listDeleted(long userId) {
        return this.baseMapper.selectDeletedPapers(userId).stream()
                .map(PaperVO::objToVo)
                .toList();

    }

    /**
     * 还原论文
     * 
     * @param id       论文 id
     * @param folderId 目标文件夹 id
     * @return 结果
     */
    @Override
    public boolean restore(long id, long folderId) {
        return this.baseMapper.restorePaper(id, folderId) > 0;
    }

    /**
     * 彻底删除论文
     * 
     * @param id 论文 id
     * @return 删除结果
     */
    @Override
    public boolean physicalDelete(long id) {
        return this.baseMapper.physicalDeletePaper(id) > 0;
    }

    /**
     * 分页查询公开论文
     *
     * @param paperQueryRequest 查询请求
     * @param request           HTTP 请求
     * @return 分页结果
     */
    @Override
    public BaseResponse<Page<PaperVO>> listPublicPaperByPage(PaperQueryRequest paperQueryRequest,
            HttpServletRequest request) {
        long current = paperQueryRequest.getPageNum();
        long size = paperQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 构造查询条件
        QueryWrapper<PaperInfo> queryWrapper = this.getQueryWrapper(paperQueryRequest);
        // 强制只查公开的
        queryWrapper.eq("is_public", 1);

        Page<PaperInfo> paperInfoPage = this.page(new Page<>(current, size), queryWrapper);

        Page<PaperVO> paperVOPage = new Page<>(current, size, paperInfoPage.getTotal());

        List<PaperVO> paperVOList = paperInfoPage.getRecords().stream().map(PaperVO::objToVo)
                .collect(Collectors.toList());
        paperVOPage.setRecords(paperVOList);

        return ResultUtils.success(paperVOPage);
    }
}
