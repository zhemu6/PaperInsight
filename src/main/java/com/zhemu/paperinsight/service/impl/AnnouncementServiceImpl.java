package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.mapper.AnnouncementMapper;
import com.zhemu.paperinsight.mapper.AnnouncementReadMapper;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementAdminQueryRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementPublishRequest;
import com.zhemu.paperinsight.model.dto.announcement.AnnouncementQueryRequest;
import com.zhemu.paperinsight.model.entity.Announcement;
import com.zhemu.paperinsight.model.entity.AnnouncementRead;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.enums.AnnouncementStatusEnum;
import com.zhemu.paperinsight.model.vo.AnnouncementAdminVO;
import com.zhemu.paperinsight.model.vo.AnnouncementVO;
import com.zhemu.paperinsight.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final AnnouncementReadMapper announcementReadMapper;


    @Override
    public Page<AnnouncementVO> listPublishedByPage(AnnouncementQueryRequest request, long userId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();

        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", AnnouncementStatusEnum.Published.getValue());
        queryWrapper.le("publish_time", LocalDateTime.now());

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("publish_time").orderByDesc("id");
        }

        Page<Announcement> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<Announcement> announcements = page.getRecords();

        Set<Long> readIds = new HashSet<>();
        if (CollUtil.isNotEmpty(announcements)) {
            List<Long> ids = announcements.stream().map(Announcement::getId).toList();
            QueryWrapper<AnnouncementRead> readWrapper = new QueryWrapper<>();
            readWrapper.eq("user_id", userId);
            readWrapper.in("announcement_id", ids);
            List<AnnouncementRead> reads = announcementReadMapper.selectList(readWrapper);
            reads.forEach(r -> readIds.add(r.getAnnouncementId()));
        }

        Page<AnnouncementVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        List<AnnouncementVO> voList = announcements.stream().map(a -> {
            AnnouncementVO vo = AnnouncementVO.objToVo(a);
            vo.setRead(readIds.contains(a.getId()));
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public long unreadCount(long userId) {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id");
        queryWrapper.eq("status", AnnouncementStatusEnum.Published.getValue());
        queryWrapper.le("publish_time", LocalDateTime.now());
        List<Announcement> list = this.list(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        List<Long> ids = list.stream().map(Announcement::getId).toList();

        QueryWrapper<AnnouncementRead> readWrapper = new QueryWrapper<>();
        readWrapper.eq("user_id", userId);
        readWrapper.in("announcement_id", ids);
        long readCount = announcementReadMapper.selectCount(readWrapper);
        return Math.max(0, ids.size() - readCount);
    }

    @Override
    public boolean markRead(long announcementId, long userId) {
        ThrowUtils.throwIf(announcementId <= 0, ErrorCode.PARAMS_ERROR);

        AnnouncementRead read = AnnouncementRead.builder()
                .announcementId(announcementId)
                .userId(userId)
                .readTime(LocalDateTime.now())
                .build();
        try {
            announcementReadMapper.insert(read);
            return true;
        } catch (DuplicateKeyException e) {
            return true;
        }
    }

    @Override
    public Page<AnnouncementAdminVO> adminListByPage(AnnouncementAdminQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();

        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("update_time").orderByDesc("id");
        }

        Page<Announcement> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        Page<AnnouncementAdminVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(AnnouncementAdminVO::objToVo).toList());
        return voPage;
    }

    @Override
    public boolean publish(AnnouncementPublishRequest request, Long userId) {

        Announcement announcement = this.getById(request.getId());
        ThrowUtils.throwIf(announcement == null, ErrorCode.NOT_FOUND_ERROR);

        if (Boolean.TRUE.equals(request.getPublished())) {
            announcement.setStatus(AnnouncementStatusEnum.Published.getValue());
            announcement.setPublishTime(LocalDateTime.now());
            announcement.setPublisherId(userId);
        } else {
            announcement.setStatus(AnnouncementStatusEnum.Draft.getValue());
            announcement.setPublishTime(null);
            announcement.setPublisherId(userId);
        }

        return  this.updateById(announcement);
    }
}
