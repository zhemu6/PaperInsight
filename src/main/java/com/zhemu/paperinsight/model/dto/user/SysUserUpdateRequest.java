package com.zhemu.paperinsight.model.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新请求
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-24   10:03
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUserUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id 待更新用户id
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 用户昵称
     */
    @Size(max = 256, message = "用户昵称过长")
    private String userName;

    /**
     * 邮箱
     */
    @Size(max = 256, message = "邮箱长度过长")
    private String email;

    /**
     * 用户头像
     */
    @Size(max = 1024, message = "头像链接过长")
    private String userAvatar;

    /**
     * 简介
     */
    @Size(max = 512, message = "简介过长")
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 状态
     */
    private Integer userStatus;

}
