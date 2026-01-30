package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementAdminQueryRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementPublishRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementQueryRequest;
import com.zhemu.paperinsight.model.entity.Announcement;
import com.zhemu.paperinsight.model.vo.AnnouncementAdminVO;
import com.zhemu.paperinsight.model.vo.AnnouncementVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author lushihao
 */
public interface AnnouncementService extends IService<Announcement> {

    Page<AnnouncementVO> listPublishedByPage(AnnouncementQueryRequest request, long userId);

    long unreadCount(long userId);

    boolean markRead(long announcementId, long userId);

    Page<AnnouncementAdminVO> adminListByPage(AnnouncementAdminQueryRequest request);

    boolean publish(AnnouncementPublishRequest request, Long userId);
}
