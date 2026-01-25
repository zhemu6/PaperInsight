package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.constant.Constants;
import com.zhemu.paperinsight.constant.RedisConstants;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.user.*;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.enums.LoginTypeEnum;
import com.zhemu.paperinsight.model.enums.UserStatusEnum;
import com.zhemu.paperinsight.model.vo.SysUserVO;
import com.zhemu.paperinsight.service.MailService;
import com.zhemu.paperinsight.service.SysUserService;
import com.zhemu.paperinsight.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zhemu.paperinsight.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author lushihao
 * @description 针对表【sys_user(用户表)】的数据库操作Service实现
 * @createDate 2026-01-23 23:16:01
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 获取登录用户
     *
     * @param request
     * @return
     */
    @Override
    public SysUser getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        SysUser currentUser = (SysUser) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 即使 Session 里有，也建议查库校验一下（防止用户刚被删，Session 还没过期）
        SysUser user = this.getById(currentUser.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    @Override
    public long userRegister(SysUserRegisterRequest sysUserRegisterRequest) {
        String userAccount = sysUserRegisterRequest.getUserAccount();
        String userPassword = sysUserRegisterRequest.getUserPassword();
        String checkPassword = sysUserRegisterRequest.getCheckPassword();
        String code = sysUserRegisterRequest.getCode();
        String email = sysUserRegisterRequest.getEmail();
        // 1. 校验
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_EMAIL_CODE + email);
        ThrowUtils.throwIf(StrUtil.isBlank(redisCode), ErrorCode.PARAMS_ERROR, "验证码已过期或不存在");
        ThrowUtils.throwIf(!redisCode.equals(code), ErrorCode.PARAMS_ERROR, "验证码错误");
        // 删除验证码
        stringRedisTemplate.delete(RedisConstants.LOGIN_EMAIL_CODE + email);

        // 2. 检查是否重复
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.NOT_FOUND_ERROR, "账号重复");
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        SysUser user = new SysUser();
        user.setUserAccount(userAccount);
        user.setEmail(email);
        user.setUserPassword(encryptPassword);
        // 生成随机昵称
        user.setUserName("初始用户_" + RandomUtil.randomString(6));
        // 注册状态默认为待审核 (Review)
        user.setUserStatus(UserStatusEnum.Review.getValue());
        // 落库
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录封装类
     * @param request          请求
     * @return 脱敏后的用户信息
     */
    @Override
    public SysUserVO userLogin(SysUserLoginRequest userLoginRequest, HttpServletRequest request) {
        String loginType = userLoginRequest.getType();
        LoginTypeEnum enumByValue = LoginTypeEnum.getEnumByValue(loginType);
        ThrowUtils.throwIf(ObjectUtil.isNull(enumByValue), ErrorCode.PARAMS_ERROR, "请选择正确的登录方式");
        SysUser user = null;

        if (LoginTypeEnum.Account.getValue().equals(enumByValue.getValue())) {
            // 如果是账密登录
            String userAccount = userLoginRequest.getUserAccount();
            String userPassword = userLoginRequest.getUserPassword();
            ThrowUtils.throwIf(StrUtil.isBlank(userAccount), ErrorCode.PARAMS_ERROR, "账号不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(userPassword), ErrorCode.PARAMS_ERROR, "密码不能为空");
            // 1. 密码加密 (和注册时用相同的算法)
            String encryptPassword = this.getEncryptPassword(userPassword);
            // 查询用户是否存在
            QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_account", userAccount);
            queryWrapper.eq("user_password", encryptPassword);
            user = this.baseMapper.selectOne(queryWrapper);
            // 如果查到的用户为空
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        } else {
            // 邮箱验证码登录
            String email = userLoginRequest.getEmail();
            String code = userLoginRequest.getCode();
            ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");
            // 校验验证码
            String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_EMAIL_CODE + email);
            ThrowUtils.throwIf(StrUtil.isBlank(redisCode) || !redisCode.equals(code), ErrorCode.PARAMS_ERROR,
                    "验证码错误或已过期");
            // 查询用户
            QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            user = this.baseMapper.selectOne(queryWrapper);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "该邮箱未注册");
            // 登录成功后删除验证码
            stringRedisTemplate.delete(RedisConstants.LOGIN_EMAIL_CODE + email);
        }
        ThrowUtils.throwIf(user.getUserStatus() == UserStatusEnum.Baned.getValue(), ErrorCode.PARAMS_ERROR,
                "账号已禁用，请联系系统管理员");
        ThrowUtils.throwIf(user.getUserStatus() == UserStatusEnum.Review.getValue(), ErrorCode.PARAMS_ERROR,
                "账号待审核，请联系系统管理员");
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息
        return SysUserVO.objToVo(user);
    }

    /**
     * 用户忘记密码
     * 
     * @param userPasswordResetRequest 忘记密码请求
     * @return
     */
    @Override
    public boolean resetPassword(SysUserPasswordResetRequest userPasswordResetRequest, HttpServletRequest request) {
        String code = userPasswordResetRequest.getCode();
        String newPassword = userPasswordResetRequest.getNewPassword();
        String checkPassword = userPasswordResetRequest.getCheckPassword();
        String email = userPasswordResetRequest.getEmail();

        // 1. 校验参数
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");
        // 2. 校验验证码
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_EMAIL_CODE + email);
        ThrowUtils.throwIf(StrUtil.isBlank(redisCode) || !redisCode.equals(code), ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        // 3.查询用户
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        SysUser user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
        // 4. 更新密码
        String encryptPassword = getEncryptPassword(newPassword);
        user.setUserPassword(encryptPassword);
        boolean result = this.updateById(user);
        // 5. 删除验证码
        if (result) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_EMAIL_CODE + email);
        }
        return result;
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 是否成功注销
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1.判断是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.OPERATION_ERROR, "未登录");
        // 2. 移除session 退出登录
        request.getSession().invalidate();
        return true;
    }

    /**
     * 更新用户邮箱
     *
     * @param request     更新请求
     * @param httpRequest HTTP请求
     * @return 是否成功
     */
    @Override
    public boolean updateUserEmail(SysUserUpdateEmailRequest request, HttpServletRequest httpRequest) {
        String newEmail = request.getNewEmail();
        String code = request.getCode();
        // 1. 获取当前登录用户
        SysUser loginUser = getLoginUser(httpRequest);
        // 2.验证验证码
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_EMAIL_CODE + newEmail);
        ThrowUtils.throwIf(StrUtil.isBlank(redisCode) || !redisCode.equals(code), ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        // 3. 检查邮箱是否已存在
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", newEmail);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被其他账号绑定");
        // 4. 更新邮箱
        loginUser.setEmail(newEmail);
        boolean result = updateById(loginUser);
        // 5. 清除验证码
        if (result) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_EMAIL_CODE + newEmail);
            // 更新 Session 中的用户信息
            httpRequest.getSession().setAttribute(USER_LOGIN_STATE, loginUser);
        }
        return result;
    }

    /**
     * 更新密码
     *
     * @param request     请求
     * @param httpRequest HTTP请求
     * @return 是否成功
     */
    @Override
    public boolean updateUserPassword(SysUserUpdatePasswordRequest request, HttpServletRequest httpRequest) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String checkPassword = request.getCheckPassword();
        SysUser loginUser = getLoginUser(httpRequest);
        // 1. 校验参数
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        // 2. 校验旧密码是否正确
        String encryptOldPassword = getEncryptPassword(oldPassword);
        ThrowUtils.throwIf(!encryptOldPassword.equals(loginUser.getUserPassword()), ErrorCode.PARAMS_ERROR,
                "旧密码错误，请重新输入");
        // 3. 更新密码
        String encryptNewPassword = getEncryptPassword(newPassword);
        loginUser.setUserPassword(encryptNewPassword);
        boolean result = updateById(loginUser);
        // 4. 更新session 重置密码后 session 失效 重新登录
        if (result) {
            httpRequest.getSession().invalidate();
        }
        return result;
    }

    @Override
    public List<SysUserVO> getUserVOList(List<SysUser> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(SysUserVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<SysUser> getQueryWrapper(SysUserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        Integer userStatus = userQueryRequest.getUserStatus();

        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.eq(userStatus != null, "user_status", userStatus);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }



    /**
     * 加盐算法
     *
     * @param userPassword 原始密码
     * @return 加盐后的密码
     */
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        return DigestUtils.md5DigestAsHex((Constants.SALT + userPassword).getBytes());
    }

}
