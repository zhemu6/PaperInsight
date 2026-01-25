package com.zhemu.paperinsight.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.DeleteRequest;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.folder.FolderAddRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderQueryRequest;
import com.zhemu.paperinsight.model.dto.folder.FolderUpdateRequest;
import com.zhemu.paperinsight.model.vo.FolderVO;
import com.zhemu.paperinsight.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文件夹管理接口
 * @author lushihao
 */
@RestController
@RequestMapping("/folder")
@Slf4j
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    /**
     * 创建文件夹
     *
     * @param folderAddRequest 文件夹添加请求
     * @return 操作结果
     */
    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<Long> addFolder(@RequestBody @Validated FolderAddRequest folderAddRequest) {
        ThrowUtils.throwIf(folderAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = folderService.addFolder(folderAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 删除文件夹
     * 可选参数 recursive: true 递归删除
     *
     * @param deleteRequest 删除请求
     * @return 操作结果
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> deleteFolder(@RequestBody @Validated DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = folderService.deleteFolder(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新文件夹
     *
     * @param folderUpdateRequest 更新请求
     * @return 操作结果
     */
    @PostMapping("/update")
    @AuthCheck
    public BaseResponse<Boolean> updateFolder(@RequestBody @Validated FolderUpdateRequest folderUpdateRequest) {
        ThrowUtils.throwIf(folderUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = folderService.updateFolder(folderUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询文件夹
     *
     * @param folderQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck
    public BaseResponse<Page<FolderVO>> listFolderByPage(@RequestBody FolderQueryRequest folderQueryRequest) {
        ThrowUtils.throwIf(folderQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<FolderVO> folderVOPage = folderService.listFolderVOByPage(folderQueryRequest);
        return ResultUtils.success(folderVOPage);
    }
}
