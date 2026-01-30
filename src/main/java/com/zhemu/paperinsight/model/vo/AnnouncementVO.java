package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.zhemu.paperinsight.model.entity.Announcement;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AnnouncementVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private LocalDateTime publishTime;

    /**
     * 是否已读
     */
    private Boolean read;

    private static final long serialVersionUID = 1L;

    public static AnnouncementVO objToVo(Announcement announcement) {
        if (announcement == null) {
            return null;
        }
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtil.copyProperties(announcement, vo);
        return vo;
    }
}
