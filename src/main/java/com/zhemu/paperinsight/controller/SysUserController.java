package com.zhemu.paperinsight.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.DeleteRequest;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.constant.UserConstant;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.user.*;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.vo.SysUserVO;
import com.zhemu.paperinsight.service.MailService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-23 23:40
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class SysUserController {

    private final SysUserService userService;
    private final MailService mailService;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    /**
     * 获取访客统计数据
     *
     * @return map: {todayVisitCount: long, totalVisitCount: long}
     */
    @GetMapping("/visit-count")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<java.util.Map<String, Long>> getVisitStats() {
        String date = cn.hutool.core.date.DateUtil.today();
        String dailyKey = com.zhemu.paperinsight.constant.RedisConstants.VISITOR_DAILY_KEY + date;
        String totalKey = com.zhemu.paperinsight.constant.RedisConstants.VISITOR_TOTAL_KEY;

        Long todayCount = stringRedisTemplate.opsForHyperLogLog().size(dailyKey);
        Long totalCount = stringRedisTemplate.opsForHyperLogLog().size(totalKey);

        java.util.Map<String, Long> result = new java.util.HashMap<>();
        result.put("todayVisitCount", todayCount == null ? 0 : todayCount);
        result.put("totalVisitCount", totalCount == null ? 0 : totalCount);

        return ResultUtils.success(result);
    }

    /**
     * 发送验证码
     *
     * @param email 用户邮箱/账号
     * @return Boolean
     */
    @PostMapping("/code/send")
    public BaseResponse<Boolean> sendCode(String email) {
        mailService.sendCode(email);
        return ResultUtils.success(true);
    }

    /**
     * 用户注册
     *
     * @param sysUserRegisterRequest 用户注册请求
     * @return 用户的注册id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody @Validated SysUserRegisterRequest sysUserRegisterRequest) {
        log.info("用户注册功能，请求参数为：{}", sysUserRegisterRequest);
        long userId = userService.userRegister(sysUserRegisterRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录 支持账密登录/邮箱验证码登录
     *
     * @param userLoginRequest 用户登陆请求封装类
     * @param request          HTTP请求对象
     * @return 登录成功后的用户信息VO
     */
    @PostMapping("/login")
    public BaseResponse<SysUserVO> userLogin(@RequestBody @Validated SysUserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        log.info("用户登录功能，请求参数为：{}", userLoginRequest);
        SysUserVO userVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userVO);
    }

    /**
     * 重置密码
     *
     * @param userPasswordResetRequest 重置密码请求
     * @return 是否重置成功
     */
    @PostMapping("/password/reset")
    public BaseResponse<Boolean> resetPassword(
            @RequestBody @Validated SysUserPasswordResetRequest userPasswordResetRequest, HttpServletRequest request) {
        boolean result = userService.resetPassword(userPasswordResetRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 脱敏后的用户
     */
    @GetMapping("/get/login")
    @AuthCheck
    public BaseResponse<SysUserVO> getLoginUser(HttpServletRequest request) {
        log.info("获取当前登录用户，请求参数为：{}", request);
        SysUser loginUser = userService.getLoginUser(request);
        return ResultUtils.success(SysUserVO.objToVo(loginUser));
    }

    /**
     * 用戶注销
     *
     * @param request 请求
     * @return 是否注销成功
     */
    @PostMapping("/logout")
    @AuthCheck
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取用户（管理员）
     *
     * @param id 用户id
     * @return User 未脱敏
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SysUser> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        SysUser user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类用户 普通用户
     *
     * @param id 用户id
     * @return UserVO 包装类
     */
    @GetMapping("/get/vo")
    @AuthCheck
    public BaseResponse<SysUserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        BaseResponse<SysUser> response = getUserById(id);
        SysUser user = response.getData();
        return ResultUtils.success(SysUserVO.objToVo(user));
    }

    /**
     * 删除用户 （管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody @Validated DeleteRequest deleteRequest) {
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户 （管理员）
     *
     * @param userUpdateRequest 用户更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody @Validated SysUserUpdateRequest userUpdateRequest) {
        SysUser user = new SysUser();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新数据失败，请稍后重试~");
        return ResultUtils.success(true);
    }
    /**
     * 更新当前登录用户的个人信息
     *
     * @param userUpdateRequest 用户更新请求
     * @param request           HTTP请求
     * @return 是否更新成功
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyInfo(@RequestBody @Validated SysUserUpdateRequest userUpdateRequest,
                                              HttpServletRequest request) {
        SysUser loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 只允许更新自己的信息
        Long userId = userUpdateRequest.getId();
        ThrowUtils.throwIf(userId == null || !userId.equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只能修改自己的信息");

        // 只允许更新部分字段:用户名、头像、简介
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUserName(userUpdateRequest.getUserName());
        user.setUserAvatar(userUpdateRequest.getUserAvatar());
        user.setUserProfile(userUpdateRequest.getUserProfile());

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新数据失败,请稍后重试~");
        return ResultUtils.success(true);
    }
    /**
     * 更新邮箱
     *
     * @param request     请求
     * @param httpRequest HTTP请求
     * @return 是否成功
     */
    @PostMapping("/update/email")
    @AuthCheck
    public BaseResponse<Boolean> updateUserEmail(@RequestBody @Validated SysUserUpdateEmailRequest request,
            HttpServletRequest httpRequest) {
        boolean result = userService.updateUserEmail(request, httpRequest);
        return ResultUtils.success(result);
    }

    /**
     * 更新密码(利用旧密码)
     *
     * @param request     请求
     * @param httpRequest HTTP请求
     * @return 是否成功
     */
    @PostMapping("/update/password")
    @AuthCheck
    public BaseResponse<Boolean> updateUserPassword(@RequestBody @Validated SysUserUpdatePasswordRequest request,
            HttpServletRequest httpRequest) {
        boolean result = userService.updateUserPassword(request, httpRequest);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询用户视图类 给前端
     *
     * @param userQueryRequest 查询请求封装类
     * @param request          HTTP请求对象
     * @return 用户VO分页对象
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SysUserVO>> listUserVOByPage(@RequestBody @Validated SysUserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();

        QueryWrapper<SysUser> queryWrapper = userService.getQueryWrapper(userQueryRequest);

        Page<SysUser> userPage = userService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据脱敏
        Page<SysUserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<SysUserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
