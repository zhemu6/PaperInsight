package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.mapper.NotificationMapper;
import com.zhemu.paperinsight.model.dto.notification.NotificationQueryRequest;
import com.zhemu.paperinsight.model.entity.Notification;
import com.zhemu.paperinsight.model.vo.NotificationVO;
import com.zhemu.paperinsight.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public Page<NotificationVO> listByPage(NotificationQueryRequest request, long userId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();

        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        if (Boolean.TRUE.equals(request.getUnreadOnly())) {
            // 只看未读
            queryWrapper.isNull("read_time");
        }

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("create_time");
        }

        Page<Notification> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        Page<NotificationVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        List<NotificationVO> records = page.getRecords().stream().map(NotificationVO::objToVo).toList();
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public long unreadCount(long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.isNull("read_time");
        return this.count(queryWrapper);
    }

    @Override
    public boolean markRead(long id, long userId) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        UpdateWrapper<Notification> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.eq("user_id", userId);
        updateWrapper.isNull("read_time");
        updateWrapper.set("read_time", LocalDateTime.now());
        // 幂等：若已读则 update=0 也算成功
        this.update(updateWrapper);
        return true;
    }

    @Override
    public boolean markReadBatch(List<Long> ids, long userId) {
        if (CollUtil.isEmpty(ids)) {
            return true;
        }
        UpdateWrapper<Notification> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids);
        updateWrapper.eq("user_id", userId);
        updateWrapper.isNull("read_time");
        updateWrapper.set("read_time", LocalDateTime.now());
        this.update(updateWrapper);
        return true;
    }

    @Override
    public boolean markAllRead(long userId) {
        UpdateWrapper<Notification> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        updateWrapper.isNull("read_time");
        updateWrapper.set("read_time", LocalDateTime.now());
        this.update(updateWrapper);
        return true;
    }

    /**
     * 创建通知（dedupKey 唯一时具备幂等能力）
     */
    public boolean createNotification(Notification notification) {
        ThrowUtils.throwIf(notification == null, ErrorCode.PARAMS_ERROR);
        try {
            return this.save(notification);
        } catch (DuplicateKeyException e) {
            // dedupKey 冲突时视为已存在
            return true;
        }
    }
}
