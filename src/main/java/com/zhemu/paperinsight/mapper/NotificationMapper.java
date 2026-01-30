package com.zhemu.paperinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhemu.paperinsight.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

}
