package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.model.dto.paper.PaperAddRequest;
import com.zhemu.paperinsight.model.dto.paper.PaperQueryRequest;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.vo.PaperDetailVO;
import com.zhemu.paperinsight.model.vo.PaperVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author lushihao
 * @description 针对表【paper_info(论文信息表)】的数据库操作Service
 * @createDate 2026-01-23 23:24:49
 */
public interface PaperInfoService extends IService<PaperInfo> {

    /**
     * 获取用户已删除的论文
     */
    List<PaperVO> listDeleted(long userId);

    /**
     * 还原论文
     */
    boolean restore(long id, long folderId);

    /**
     * 物理删除论文
     */
    boolean physicalDelete(long id);

    Long addPaper(PaperAddRequest paperAddRequest, HttpServletRequest request);

    QueryWrapper<PaperInfo> getQueryWrapper(PaperQueryRequest paperQueryRequest);

    /**
     * 分页查询公开论文
     *
     * @param paperQueryRequest 查询请求
     * @param request           HTTP 请求
     * @return 分页结果
     */
    BaseResponse<Page<PaperVO>> listPublicPaperByPage(PaperQueryRequest paperQueryRequest, HttpServletRequest request);

    /**
     * 获取论文详情 (包含分析结果)
     *
     * @param id     论文ID
     * @param userId 当前用户ID (用于查看权限或私有论文)
     * @return 论文详情
     */
    PaperDetailVO getPaperDetail(long id, long userId);
}
