package com.zhemu.paperinsight.controller;

import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.recycle.RecycleRequest;
import com.zhemu.paperinsight.model.entity.Folder;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.vo.PaperVO;
import com.zhemu.paperinsight.service.FolderService;
import com.zhemu.paperinsight.service.PaperInfoService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回收站管理接口
 *
 * @author lushihao
 */
@RestController
@RequestMapping("/recycle")
@Slf4j
@RequiredArgsConstructor
public class RecycleBinController {
    private final FolderService folderService;
    private final PaperInfoService paperInfoService;
    private final SysUserService userService;

    /**
     * 获取回收站列表 里面只存储文章
     *
     * @param request 获取用户
     * @return 操作结果
     */
    @PostMapping("/list")
    @AuthCheck
    public BaseResponse<List<PaperVO>> listRecycleBin(HttpServletRequest request) {
        SysUser loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 2. 获取 用户删除的论文列表
        List<PaperVO> deletedPapers = paperInfoService.listDeleted(userId);
        return ResultUtils.success(deletedPapers);
    }

    /**
     * 还原项目
     *
     * @param recycleRequest 还原请求
     * @return 还原结果
     */
    @PostMapping("/restore")
    @AuthCheck
    public BaseResponse<Boolean> restore(@RequestBody @Validated RecycleRequest recycleRequest) {
        Long paperId = recycleRequest.getPaperId();
        Long folderId = recycleRequest.getFolderId();

        // 如果未指定文件夹，默认还原到根目录 (0)
        if (folderId == null) {
            folderId = 0L;
        }

        // 1. 校验目标文件夹 (0代表根目录，无需查询)
        if (folderId != 0L) {
            Folder folder = folderService.getById(folderId);
            ThrowUtils.throwIf(folder == null, ErrorCode.NOT_FOUND_ERROR, "选择的文件夹不存在");
        }

        // 2. 还原 (getById无法查到已删除对象，所以直接尝试还原，根据结果判断)
        boolean result = paperInfoService.restore(paperId, folderId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "还原失败，文章可能不存在或已彻底删除");

        return ResultUtils.success(true);
    }

    /**
     * 彻底删除项目
     *
     * @param recycleRequest 彻底删除请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> physicalDelete(@RequestBody @Validated RecycleRequest recycleRequest) {
        Long paperId = recycleRequest.getPaperId();
        boolean result = paperInfoService.physicalDelete(paperId);
        return ResultUtils.success(result);
    }
}
