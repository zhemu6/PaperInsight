package com.zhemu.paperinsight.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.DeleteRequest;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementAdminQueryRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementCreateRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementPublishRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementUpdateRequest;
import com.zhemu.paperinsight.model.entity.Announcement;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.enums.AnnouncementStatusEnum;
import com.zhemu.paperinsight.model.vo.AnnouncementAdminVO;
import com.zhemu.paperinsight.service.AnnouncementService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 系统公告-管理员
 * @author lushihao
 */
@RestController
@RequestMapping("/admin/announcement")
@Slf4j
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final SysUserService userService;

    /**
     * 管理员创建公告
     * @param request 创建公告请求
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/create")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> create(@RequestBody @Validated AnnouncementCreateRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(httpServletRequest).getId();

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(AnnouncementStatusEnum.Draft.getValue())
                .publisherId(userId)
                .build();
        boolean ok = announcementService.save(announcement);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(announcement.getId());
    }

    /**
     * 管理员更新公告
     * @param request 更新公告请求
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> update(@RequestBody @Validated AnnouncementUpdateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Announcement announcement = announcementService.getById(request.getId());
        ThrowUtils.throwIf(announcement == null, ErrorCode.NOT_FOUND_ERROR);

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        boolean ok = announcementService.updateById(announcement);
        return ResultUtils.success(ok);
    }

    /**
     * 删除公告请求
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> delete(@RequestBody @Validated DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean ok = announcementService.removeById(deleteRequest.getId());
        return ResultUtils.success(ok);
    }

    /**
     * 管理员发布公告
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/publish")
    @AuthCheck(mustRole = "admin")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> publish(@RequestBody @Validated AnnouncementPublishRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(httpServletRequest).getId();
        boolean ok = announcementService.publish(request,userId);

        return ResultUtils.success(ok);
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<AnnouncementAdminVO>> listByPage(@RequestBody AnnouncementAdminQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<AnnouncementAdminVO> page = announcementService.adminListByPage(request);
        return ResultUtils.success(page);
    }
}
