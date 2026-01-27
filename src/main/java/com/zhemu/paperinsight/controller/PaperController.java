package com.zhemu.paperinsight.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.DeleteRequest;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.paper.PaperAddRequest;
import com.zhemu.paperinsight.model.dto.paper.PaperQueryRequest;
import com.zhemu.paperinsight.model.dto.paper.PaperUpdateRequest;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.vo.PaperVO;
import com.zhemu.paperinsight.service.PaperInfoService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhemu.paperinsight.model.vo.PaperDetailVO;
import com.zhemu.paperinsight.model.entity.SysUser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 论文管理接口
 * 
 * @author lushihao
 */
@RestController
@RequestMapping("/paper")
@Slf4j
@RequiredArgsConstructor
public class PaperController {

    private final PaperInfoService paperInfoService;
    private final SysUserService userService;

    /**
     * 添加论文请求
     * 
     * @param paperAddRequest 论文添加请求
     * @param request         请求
     * @return 是否添加成功
     */
    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<Long> addPaper(@RequestBody @Validated PaperAddRequest paperAddRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(paperAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long paperId = paperInfoService.addPaper(paperAddRequest, request);
        return ResultUtils.success(paperId);
    }

    /**
     * 用户删除论文
     * 
     * @param deleteRequest 删除请求
     * @return 操作结果
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> deletePaper(@RequestBody @Validated DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = paperInfoService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 用户更新论文
     * 
     * @param paperUpdateRequest 更新请求
     * @return 操作结果
     */
    @PostMapping("/update")
    @AuthCheck
    public BaseResponse<Boolean> updatePaper(@RequestBody @Validated PaperUpdateRequest paperUpdateRequest) {
        ThrowUtils.throwIf(paperUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        PaperInfo paperInfo = new PaperInfo();
        BeanUtils.copyProperties(paperUpdateRequest, paperInfo);
        boolean result = paperInfoService.updateById(paperInfo);
        return ResultUtils.success(result);
    }

    /**
     * 分页罗列论文
     * 
     * @param paperQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck
    public BaseResponse<Page<PaperVO>> listPaperByPage(@RequestBody PaperQueryRequest paperQueryRequest) {
        ThrowUtils.throwIf(paperQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = paperQueryRequest.getPageNum();
        long size = paperQueryRequest.getPageSize();

        QueryWrapper<PaperInfo> queryWrapper = paperInfoService.getQueryWrapper(paperQueryRequest);

        Page<PaperInfo> paperInfoPage = paperInfoService.page(new Page<>(current, size), queryWrapper);
        // 数据脱敏
        Page<PaperVO> paperVOPage = new Page<>(current, size, paperInfoPage.getTotal());
        List<PaperVO> paperVOList = paperInfoPage.getRecords().stream()
                .map(PaperVO::objToVo)
                .collect(Collectors.toList());
        paperVOPage.setRecords(paperVOList);
        return ResultUtils.success(paperVOPage);
    }

    /**
     * 分页查询公开论文 (论文广场)
     *
     * @param paperQueryRequest
     * @return
     */
    @PostMapping("/list/public/page")
    public BaseResponse<Page<PaperVO>> listPublicPaperByPage(@RequestBody PaperQueryRequest paperQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(paperQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return paperInfoService.listPublicPaperByPage(paperQueryRequest, request);
    }

    /**
     * 获取论文详情
     *
     * @param id 论文 ID
     * @return 论文详情
     */
    @GetMapping("/get")
    @AuthCheck
    public BaseResponse<PaperDetailVO> getPaperDetail(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(request).getId();
        PaperDetailVO vo = paperInfoService.getPaperDetail(id, userId);
        return ResultUtils.success(vo);
    }
}
