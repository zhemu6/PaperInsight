package com.zhemu.paperinsight.model.dto.notification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量已读
 * @author lushihao
 */
@Data
public class NotificationReadBatchRequest {

    @NotEmpty
    private List<Long> ids;
}
