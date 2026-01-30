package com.zhemu.paperinsight.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.notification.NotificationQueryRequest;
import com.zhemu.paperinsight.model.dto.notification.NotificationReadBatchRequest;
import com.zhemu.paperinsight.model.dto.notification.NotificationReadRequest;
import com.zhemu.paperinsight.model.vo.NotificationVO;
import com.zhemu.paperinsight.service.NotificationService;
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
 * @author lushihao
 */
@RestController
@RequestMapping("/notification")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SysUserService userService;

    /**
     * 分页罗列登录用户的请求
     * @param request 通知查询请求
     * @param httpServletRequest 登录用户
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck
    public BaseResponse<Page<NotificationVO>> listByPage(@RequestBody NotificationQueryRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        Page<NotificationVO> page = notificationService.listByPage(request, userId);
        return ResultUtils.success(page);
    }

    /**
     * 统计多少条未读
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/unread/count")
    @AuthCheck
    public BaseResponse<Long> unreadCount(HttpServletRequest httpServletRequest) {
        long userId = userService.getLoginUser(httpServletRequest).getId();
        long count = notificationService.unreadCount(userId);
        return ResultUtils.success(count);
    }

    /**
     * 读取消息
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/read")
    @AuthCheck
    public BaseResponse<Boolean> markRead(@RequestBody @Validated NotificationReadRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        boolean ok = notificationService.markRead(request.getId(), userId);
        return ResultUtils.success(ok);
    }

    /**
     * 批量读取通知
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/read/batch")
    @AuthCheck
    public BaseResponse<Boolean> markReadBatch(@RequestBody @Validated NotificationReadBatchRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        boolean ok = notificationService.markReadBatch(request.getIds(), userId);
        return ResultUtils.success(ok);
    }

    /**
     * 全部已读
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/read/all")
    @AuthCheck
    public BaseResponse<Boolean> markAllRead(HttpServletRequest httpServletRequest) {
        long userId = userService.getLoginUser(httpServletRequest).getId();
        boolean ok = notificationService.markAllRead(userId);
        return ResultUtils.success(ok);
    }
}
