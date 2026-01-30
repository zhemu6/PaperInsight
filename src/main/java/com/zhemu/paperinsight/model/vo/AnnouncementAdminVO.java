package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.zhemu.paperinsight.model.entity.Announcement;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AnnouncementAdminVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String status;

    private LocalDateTime publishTime;

    private Long publisherId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;

    public static AnnouncementAdminVO objToVo(Announcement announcement) {
        if (announcement == null) {
            return null;
        }
        AnnouncementAdminVO vo = new AnnouncementAdminVO();
        BeanUtil.copyProperties(announcement, vo);
        return vo;
    }
}
