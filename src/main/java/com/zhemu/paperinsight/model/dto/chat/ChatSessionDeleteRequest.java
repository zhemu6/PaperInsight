package com.zhemu.paperinsight.model.dto.chat;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 对话 id 删除请求
 * @author lushihao
 */
@Data
public class ChatSessionDeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "chatId is required")
    private String chatId;
}
