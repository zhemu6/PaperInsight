package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhemu.paperinsight.model.dto.notification.NotificationQueryRequest;
import com.zhemu.paperinsight.model.entity.Notification;
import com.zhemu.paperinsight.model.vo.NotificationVO;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    Page<NotificationVO> listByPage(NotificationQueryRequest request, long userId);

    long unreadCount(long userId);

    boolean markRead(long id, long userId);

    boolean markReadBatch(List<Long> ids, long userId);

    boolean markAllRead(long userId);
}
