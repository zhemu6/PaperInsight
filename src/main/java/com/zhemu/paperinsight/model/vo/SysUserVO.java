package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.zhemu.paperinsight.model.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 返回给前端用户
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUserVO implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色 user admin
     */
    private String userRole;


    /**
     * 邮箱
     */
    private String email;

    @Serial
    private static final long serialVersionUID = 1L;

    public static SysUserVO objToVo(SysUser user) {
        if (user == null) {
            return null;
        }
        SysUserVO userVO = new SysUserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
}