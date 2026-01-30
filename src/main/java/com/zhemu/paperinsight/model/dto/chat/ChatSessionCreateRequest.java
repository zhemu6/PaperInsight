package com.zhemu.paperinsight.model.dto.chat;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 对话 session 创建请求
 * @author lushihao
 */
@Data
public class ChatSessionCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "paperId is required")
    private Long paperId;
}
