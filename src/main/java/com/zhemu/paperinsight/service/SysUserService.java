package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhemu.paperinsight.model.dto.user.*;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhemu.paperinsight.model.vo.SysUserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author lushihao
* @description 针对表【sys_user(用户表)】的数据库操作Service
* @createDate 2026-01-23 23:16:01
*/
public interface SysUserService extends IService<SysUser> {

    SysUser getLoginUser(HttpServletRequest request);

    long userRegister(SysUserRegisterRequest sysUserRegisterRequest);

    SysUserVO userLogin(SysUserLoginRequest userLoginRequest, HttpServletRequest request);

    boolean resetPassword(SysUserPasswordResetRequest userPasswordResetRequest,HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    boolean updateUserEmail(SysUserUpdateEmailRequest request, HttpServletRequest httpRequest);

    boolean updateUserPassword(SysUserUpdatePasswordRequest request, HttpServletRequest httpRequest);

    List<SysUserVO> getUserVOList(List<SysUser> records);

    QueryWrapper<SysUser> getQueryWrapper(SysUserQueryRequest userQueryRequest);

}
