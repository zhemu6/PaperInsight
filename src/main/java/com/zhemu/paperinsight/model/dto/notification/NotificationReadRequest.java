package com.zhemu.paperinsight.model.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author lushihao
 */
@Data
public class NotificationReadRequest {

    /**
     * 读取通知的 ID
     */
    @NotNull
    private Long id;
}
