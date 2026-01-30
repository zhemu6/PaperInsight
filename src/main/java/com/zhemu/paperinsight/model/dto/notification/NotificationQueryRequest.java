package com.zhemu.paperinsight.model.dto.notification;

import com.zhemu.paperinsight.common.PageRequest;
import lombok.Data;

/**
 * @author lushihao
 */
@Data
public class NotificationQueryRequest extends PageRequest {

    /**
     * 是否只看未读
     */
    private Boolean unreadOnly;
}
