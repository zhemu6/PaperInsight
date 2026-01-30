package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.zhemu.paperinsight.model.entity.Notification;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知 VO 对象
 * @author lushihao
 */
@Data
public class NotificationVO implements Serializable {

    private Long id;

    private String type;

    private String title;

    private String content;

    private String payloadJson;

    private LocalDateTime readTime;

    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;

    public static NotificationVO objToVo(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        BeanUtil.copyProperties(notification, vo);
        return vo;
    }
}
