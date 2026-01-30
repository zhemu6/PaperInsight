package com.zhemu.paperinsight.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String chatId;
    private Long paperId;
    private String title;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createTime;
}
