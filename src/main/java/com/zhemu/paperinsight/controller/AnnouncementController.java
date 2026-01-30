package com.zhemu.paperinsight.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementQueryRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementReadRequest;
import com.zhemu.paperinsight.model.vo.AnnouncementVO;
import com.zhemu.paperinsight.service.AnnouncementService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统公告-用户
 * @author lushihao
 */
@RestController
@RequestMapping("/announcement")
@Slf4j
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SysUserService userService;

    @PostMapping("/list/page")
    @AuthCheck
    public BaseResponse<Page<AnnouncementVO>> listByPage(@RequestBody AnnouncementQueryRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        Page<AnnouncementVO> page = announcementService.listPublishedByPage(request, userId);
        return ResultUtils.success(page);
    }

    /**
     * 获取未读数量
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/unread/count")
    @AuthCheck
    public BaseResponse<Long> unreadCount(HttpServletRequest httpServletRequest) {
        long userId = userService.getLoginUser(httpServletRequest).getId();
        long count = announcementService.unreadCount(userId);
        return ResultUtils.success(count);
    }

    /**
     * 标记为已读
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/read")
    @AuthCheck
    public BaseResponse<Boolean> markRead(@RequestBody @Validated AnnouncementReadRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        boolean ok = announcementService.markRead(request.getId(), userId);
        return ResultUtils.success(ok);
    }
}
